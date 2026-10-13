# Proposal

## Why

Fortuna replaces rank movement after every computed match, so when several fixtures kick off at the same time the leaderboard, My pools, History, and another gambler's Timeline only show the effect of whichever match was processed last. Gamblers need one movement per kickoff that updates as its matches settle, and History and Timeline grouped by kickoff instead of one dated row per match.

## What Changes

- Define a kickoff group as the matches of a pool's layout that share the same scheduled kickoff instant (normalized to UTC). Grouping never uses a formatted date or local time.
- Reset the rank baseline (`beforePosition`) only when a settlement opens a newer kickoff group. Later settlements of the same group, late results, and corrections of older groups keep the baseline. Leaderboard and My pools movement therefore measures the newest kickoff group with a settled match, progressively (`+3, 1 of 2` then `+2, 2 of 2`).
- Add a `trendGroup` object to pool score responses with the group's kickoff instant and its computed and total match counts. `position` and `beforePosition` keep their meaning and no field is removed, so the score contract is additive.
- **BREAKING**: History and Timeline responses become grouped feeds: one item per kickoff group, never split across pages, with the group's counts, movement, and match rows. Timeline's unsettled/finished phases move to the group level. Continuations issued by the previous finished-bets index are rejected with the existing `INVALID_CURSOR` first-page restart. Backend and both apps ship together; no old response mode or adapter is kept. History no longer accepts a search filter.
- Compute each History/Timeline group's movement on the server from the per-match position snapshots that settlement already stores, and report movement as unavailable where a snapshot is missing.
- Propagate a changed kickoff in the pool layout to every gambler's bet rows, so groups follow a rescheduled match. Today a layout date change propagates nothing.
- Group History and Timeline on iOS and Android with a shared scrolling date/time/count/trend header, a vertical group rail, and the existing match rows. Keep the shared `TrendIndicator` and the canonical-vector icon rule.
- Keep the supplied design base and the icon-corrected reference in `reference/`.
- Backfill existing pools before cutover: copy the kickoff onto computed bet rows and record each pool's current trend group.

Accepted approximations, chosen deliberately over a chronological replay engine: a late result from an older kickoff and a correction of an older group both move the newest group's trend instead of their own; a gambler who joins between two settlements of one group shifts everyone's baseline comparison; a match rescheduled after it was settled keeps snapshots taken under its old group. Each is no worse than today's per-match behavior and is documented in the design.

## Capabilities

### New Capabilities

- `kickoff-group-standings`: group identity, the baseline reset rule, the `trendGroup` score contract, group counts and movement, kickoff-change propagation, and backfill before cutover.

### Modified Capabilities

- `bet-history`: replace per-row dates with shared kickoff-group headers and movement while keeping the points summary, match actions, loading, recovery, localization, and accessibility.
- `gambler-timeline`: share the grouped History presentation with selected-gambler ownership and group-level unsettled/finished phases, preserving prediction visibility and per-match pending states.
- `pool-leaderboard`: read movement as the newest settled kickoff group's baseline against the current position, with partial, pending, and unavailable states.
- `pool-score-list`: align My pools movement and announcements with the same group result without redesigning the pool list.
- `backend-pagination`: grouped feeds return whole groups per page, Timeline phases change at group level, and the previous finished-bets continuations are invalid.

## Impact

- Backend: the position decision in the Pool domain, one attribute on the pool root, the score response, one sentinel attribute and one GSI on bet rows, the History and Timeline handlers, a layout-stream handler for kickoff changes, a maintenance backfill job, and the persistence and settlement documentation.
- iOS: DataBet/DataPool contracts, shared History/Timeline grouping and group-level paging, the group header, `TrendIndicator` inputs, localized accessibility, and canonical trend assets.
- Android: data/bet and data/pool contracts, the same grouping and paging, group presentation, `TrendIndicator` inputs, localized accessibility, and canonical trend assets.
- Unchanged: scoring formulas, prediction lock and reveal rules, rank tie-breaks, settlement idempotency, membership-generation fences, rejoin restoration, and the ranked match-bets endpoint and its cursor.
- No code is implemented by this planning change. The backend deploys first with its backfill, and the grouped endpoints and both apps cut over in one coordinated release.
