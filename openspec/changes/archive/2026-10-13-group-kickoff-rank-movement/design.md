# Design

## Context

See [proposal.md](proposal.md) for the problem and scope. Settlement runs one operation per pool through SQS FIFO ([docs/pool-settlement.md](../../../docs/pool-settlement.md)). Its positions stage ranks every membership and, on the operation's first write to a membership, stores the previous position as `beforePosition` on the membership and on the settled match detail; later recalculations in the same operation and standings reconciliations keep it. That decision is pure and lives in `PositionPolicy.decide`. Both apps derive movement as `beforePosition - position` for the leaderboard and My pools.

Finished bets are read through `GetFinishedPoolGamblerBets-index`, keyed on `computedDateTime`, so two matches with the same kickoff are only adjacent if they were computed back to back. Pending bets are read through `GetPendingPoolGamblerBets-index`, keyed on `matchDateTime`. Every bet detail carries its own copy of the layout's `matchDateTime`; the PoolLayout stream handler reacts to inserted matches and to results, and a changed `matchDateTime` propagates nothing today.

iOS and Android already share the production History row and points summary with another gambler's Timeline, and both already have a shared `TrendIndicator`. Persistence follows [docs/database-model.md](../../../docs/database-model.md): sentinel attributes gate index membership, and repositories are data access only.

## Goals / Non-Goals

**Goals:**

- One movement per kickoff group on every surface, updated as the group's matches settle, with the baseline taken once per group.
- Grouped History and Timeline served by the backend with authoritative counts, never relying on client-side grouping of computation-ordered pages.
- Additive score contract; the only breaking change is the shape of the two feeds.
- Small enough to land in one coordinated release, with a bounded backfill.

**Non-Goals:**

- Chronological replay of history, frozen comparison cohorts, revisioned publications, or evidence manifests. The approximations this leaves are listed under Risks and are each equal to or better than today's per-match behavior.
- Changing point formulas, lock/reveal rules, rank tie breaks, settlement idempotency, or rejoin restoration.
- Live polling, navigation-shell changes, or raster icons as app assets.

## Decisions

### 1. Group identity is pool plus kickoff instant

A kickoff group is identified by the pool and the layout match's scheduled instant normalized to UTC, without rounding or tolerance. The instant itself is the group key sent to clients as an ISO-8601 UTC string; clients group by string equality and never by a formatted or local date. Android must stop converting to `LocalDateTime` before comparing, since DST can make two instants display alike.

Group membership and total count come from the pool's layout: every layout match with that instant. A match is counted as computed when the layout holds a result for it; the pool's settlement of that result follows through the FIFO within the operation's duration, and the movement updates when it completes.

**Alternative:** an opaque server-generated group ID adds a lookup for no benefit; the instant is already stable and unique within a pool.

### 2. The baseline resets per kickoff group, not per settlement

The pool root gains `trendKickoffDateTime`: the kickoff instant of the group whose baseline the memberships currently hold. The positions stage already reads the root when it claims the operation, so it compares the settling match's kickoff with that value and `PositionPolicy.decide` takes one of three branches for a match settlement:

| Settling kickoff vs root | Membership `beforePosition` | Root |
| --- | --- | --- |
| Newer, or root has none | Reset to the position before this operation (today's behavior) | Set to this kickoff at completion |
| Equal | Kept, as standings reconciliation already does | Unchanged |
| Older (late result, correction) | Kept | Unchanged |

The match detail still receives its own `beforePosition` and `position` snapshot on the operation's first write, exactly as today, in every branch. The comparison is made once from the root state read at claim, so it is stable across deadline yields and retries; the root is only written at completion, fenced by the owner token, and operations are serialized per pool, so the next operation sees the committed value. Standings reconciliation and rejoin restoration keep their existing rule (`position` moves, `beforePosition` stays).

Because the root value only advances to newer kickoffs, the leaderboard automatically shows the newest group with a settled match, and a late older result cannot select its own group.

**Alternatives:** keeping today's per-settlement reset is the bug. Chronological replay from stored awards gives exact attribution for late results and corrections but needs frozen cohorts, revisioned publications, and bounded replay; it is deferred, and nothing here prevents adding it later.

### 3. Group counts are computed at read time from the layout

`totalMatches` is the number of layout matches at the group's kickoff; `computedMatches` is how many of those have a result. Every reader that emits a group (score responses, History, Timeline) queries the pool's layout partition once per request and reuses it for every group in the response; pools that share a layout share that read within a request. The layout holds at most the matches of one tournament, so the query is one page.

**Alternative:** counters on the pool root maintained at completion and by the kickoff-change handler. They save a read per request but can drift, and the feeds need the layout for group totals anyway.

### 4. The score contract gains `trendGroup` and keeps `position` and `beforePosition`

`PoolGamblerScoreResponse` adds an optional `trendGroup` object: `kickoffDateTime`, `computedMatches`, `totalMatches`. It is present when the pool root has a trend kickoff and absent otherwise. Clients keep computing movement as `beforePosition - position` and derive the display state:

| `trendGroup` | `beforePosition` and `position` | State |
| --- | --- | --- |
| absent | any | pending (no settled kickoff group yet) |
| present | both present, `computedMatches < totalMatches` | numeric movement with a partial qualifier |
| present | both present, counts equal | numeric movement, zero shown as steady |
| present | either missing | unavailable |

A response that lacks `position` for a member keeps today's em-dash behavior. No field is removed, so an older app keeps working and, incidentally, starts showing group-based movement.

**Alternative:** a required discriminated movement object with explicit reasons. It is more self-describing but forces both apps to change for the score contract and removes a field for no functional gain.

### 5. History and Timeline serve whole kickoff groups

Settlement writes `computedMatchDateTime` on the match detail, a copy of the kickoff, on the detail's first computation and keeps it on corrections. Pending details never have it, so a new GSI `GetFinishedPoolGamblerBetsByMatchDate-index` (`pk`, `computedMatchDateTime`) holds only finished rows, the same sentinel pattern the table already uses. The existing `computedDateTime` index stays in the table definition, but no code reads it after this change; dropping it is left to a later change.

History pages that index newest first. When a page boundary falls inside a group, the handler completes the group with one equality query on the same index (`computedMatchDateTime = kickoff`), so a group is never split across pages and the continuation points past the completed group. The cursor is the index cursor from the `backend-pagination` codec; a token from the old `computedDateTime` index fails its key-shape validation and is rejected with `INVALID_CURSOR`. History no longer accepts the `searchText` query parameter, and both hosts ignore it: no app sends it, and filtering a group's children would break the rule that movement comes from all of the group's computed children.

Each group item carries `kickoffDateTime`, `totalMatches`, `computedMatches`, `movement`, and `matches` (the existing match-bet row shape, ordered by match ID). `movement` is `{ beforePosition, position }` taken from the group's children: `beforePosition` from the child with the earliest `computedDateTime`, `position` from the child with the latest. It is null when either child lacks its snapshot, which clients show as unavailable. For the newest group this equals the membership's `beforePosition` and `position` unless a join or removal has since moved the current position, which is the intended distinction between a historical group and the current standing.

Timeline's unsettled phase pages the pending index (uncomputed details up to the lock horizon) grouped by kickoff. For each group it runs one unfiltered `matchDateTime = kickoff` query on that same index, which holds every detail because every detail has a `matchDateTime`. That one query returns the gambler's pending and computed children together, completes a group cut by the storage page, and also covers computed details the backfill has not reached yet. A partial group therefore shows every row together with its progressive movement. This relies on the pending index staying non-sparse; a change that removes `matchDateTime` from computed details would break it. Its finished phase pages the new index and skips any group whose layout still has a match without a result, because that group was already shown in the unsettled phase. The phase wrapper and both inner cursors keep their current codec. Child visibility follows the existing eligibility and prediction-reveal rules; counts come from the layout and never reveal a hidden prediction.

**Alternatives:** grouping on the client fails when same-kickoff matches are not adjacent in computation order and cannot know a group's full size. Revision-bound cursors that invalidate on every settlement would reset a reader's scroll on every result during a matchday.

### 6. A changed kickoff propagates like a result

The PoolLayout stream handler gains a branch for a `MODIFY` whose `matchDateTime` changed. It fans out through `GetPoolGamblerScoresByMatch-index`, the index results already use, and writes the new instant to every detail's `matchDateTime` and, when the detail is computed, `computedMatchDateTime`, each as a conditional update guarded by the old value so repeated deliveries converge. Group membership and counts follow because both are read from the layout and the copies.

The root's `trendKickoffDateTime` is not rewritten by this handler; if the moved match was the group currently tracked, the next settlement of a newer kickoff resets the baseline as usual. A match moved after it was settled keeps the snapshots taken under its old group, which the group movement rule then reads under the new group; that is listed as an accepted approximation. A rescheduled match that is auto-ingested also needs its `MatchScoreIngestion` item updated; that stays an operator step and is documented, not automated here.

**Alternative:** a versioned schedule-change pool operation that regroups and replays history. It is only needed with chronological replay.

### 7. One shared group presentation per platform

Use [reference/history-grouped-icons.png](reference/history-grouped-icons.png) as the visual base and keep [reference/history-original.png](reference/history-original.png) for provenance; the written contract controls behavior and sizing. Refactor the shared History row into matchup, prediction, and points content plus group context. A scrolling, non-sticky group header shows the localized date, the localized time with a pluralized match count, and a trailing trend pill; a one-match group can omit the count. A partial group adds a localized `1 of 2 computed` qualifier; pending and unavailable are distinct from steady. A subdued vertical rail and thin internal dividers connect the rows. Timeline passes selected-gambler ownership (`Bet`); History keeps `Your bet`. Each child keeps its match destination and press feedback; headers are not actions.

Clients render the server's groups in server order and merge nothing; a pull-to-refresh replaces the feed from the first page. Movement uses the existing `TrendIndicator` with focused inputs for the qualifier and the pending/unavailable labels; any introduced glyph follows the repository icon rule (Material Symbols where accurate, one committed canonical vector per icon, both platforms derived from it). Placeholders populate the production header and row with placeholder models through `isPlaceholder`.

The shared positive, negative, and steady trend color tokens get darker light-mode values so trend glyphs and numbers reach at least 4.5:1 on every light surface they appear on (canvas, badge fill, current-user row): `#33691E`, `#B71C1C`, and `#8C5E00`. Dark values are unchanged, and both platforms use the same values. At accessibility text sizes the leaderboard row reflows instead of clipping its movement or truncating the username and score; ordinary sizes keep the one-line ellipsized username. The append Retry on History, Timeline, and the leaderboard must retry the failed append even when only a few groups are loaded; on iOS the lazy-paging library's `retry()` repeats a prepend in that case, so the app uses its own append-only paging stack until the library retries the failed edge.

### 8. Backfill through the maintenance queue

A per-pool `BACKFILL_KICKOFF_GROUPS` maintenance job, restartable like `BACKFILL_PROJECTION`, writes `computedMatchDateTime` on every computed detail that lacks it (copied from its `matchDateTime`) and, when the root has no `trendKickoffDateTime`, sets it from the kickoff of the match in the pool's most recently completed `RESULT#` receipt. Pools settled before receipts existed have none; for them the job uses the latest kickoff among the layout matches that have a computed detail in the pool, which is the kickoff whose settlement last reset `beforePosition` under the old per-settlement rule (approximate when several matches share that kickoff). The job continues itself on the maintenance queue, and Lambda's recursive-loop detection stops such chains after about 16 hops without running the handler, so `OnPoolMaintenanceJob` and `OnUpdatePositions`, whose self-continuation is intentional and bounded, set `RecursiveLoop: Allow`. Memberships keep whatever `beforePosition` the last settlement left, so the first group after cutover may be approximate for pools mid-tournament; it corrects itself at the next newer kickoff.

## Risks / Trade-offs

- [A late result from an older kickoff arrives after a newer group settled] → Positions recalculate and the newest group's trend absorbs the change; the older group's History movement, read from snapshots, stays as it was. Documented; same as today.
- [A correction changes an older group's award] → Same as above. Corrections keep `computedDateTime`, so group child order is stable.
- [A gambler joins or is removed between two settlements of one group] → Standings reconciliation moves `position` and keeps `beforePosition`, so other members' movement includes the roster shift. Same as today.
- [A match is rescheduled after it was settled] → Its snapshots were taken under the old group; the new group's movement is approximate. Rare data fix; documented.
- [A result exists in the layout before its settlement completes] → `computedMatches` counts it a few seconds early while the movement still reflects the previous settlement. Accepted; the FIFO closes the gap on its own.
- [A settlement is in flight when Timeline is read: the layout already has the result but the detail is still pending] → Timeline classifies the group as finished from the layout counts and, for a few seconds, shows it without that row. Accepted; the next refresh after settlement completes shows the full group.
- [Legacy computed details lack position snapshots] → Their group shows unavailable movement rather than a fabricated zero.
- [Old finished-bets tokens after cutover] → Rejected with `INVALID_CURSOR`; the app restarts from page one. No tokens cross the release because the apps ship together.
- [Shared presentation changes affect both History and Timeline] → Verify ownership wording, independent summaries, per-row actions, and phase transitions together on both platforms.

## Migration Plan

1. Deploy the backend: the policy branch, root attribute, `computedMatchDateTime` writes, the new GSI, `trendGroup` on score responses, the kickoff-change handler, and the backfill job. All additive; existing apps keep working.
2. Run `BACKFILL_KICKOFF_GROUPS` for every pool and verify each pool's root has a trend kickoff and every computed detail has `computedMatchDateTime`.
3. Release the grouped History and Timeline endpoints together with the iOS and Android builds that consume them. Older app versions are not supported for those two screens after this release.
4. Rollback: restore the matched backend and app release. Added attributes and the index are harmless to the previous code.

Verification covers groups of one to four matches, mixed gains and losses, ties, partial computation, an older result arriving after a newer group, a correction, a rescheduled pending match, a group spanning a page boundary, the group-level Timeline phase change, and legacy rows without snapshots. Before authoring mobile test code, explore the relevant app paths with ARTEMIS and record observed actions; use verified dynamic locators and explicit waits, with verified coordinate fallback where the chosen framework supports it.
