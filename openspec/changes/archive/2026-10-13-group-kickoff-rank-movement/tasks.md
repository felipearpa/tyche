# Tasks

All tasks are implementation work and remain unchecked. Read this change's proposal, design, and six delta specs before starting, and `docs/database-model.md` and `docs/pool-settlement.md` before any backend task. The visual reference is `reference/history-grouped-icons.png`; its provenance is in `reference/README.md`.

## 1. Backend — baseline rule, trend group, and score contract

- [x] 1.1 Add `trendKickoffDateTime` to the pool root and the newer/equal/older kickoff decision to `PositionPolicy.decide` and the positions stage, writing the root value at completion under the owner fence; verify domain tests cover `8 → 5 → 6` keeping `beforePosition` 8, a newer kickoff resetting it, an older result or correction keeping it, and a resumed operation applying the same decision.
- [x] 1.2 Add the kickoff-group reader that loads a layout once per request and returns total and computed counts per kickoff instant (UTC-normalized); verify unit tests cover equivalent offsets, two instants on one day, and a result present before settlement.
- [x] 1.3 Add `trendGroup` to `PoolGamblerScoreResponse` for the leaderboard and My pools handlers, absent when the root has no trend kickoff; verify endpoint tests show identical values on both endpoints, absence for a never-settled pool, and unchanged `position`/`beforePosition`.
- [x] 1.4 Write `computedMatchDateTime` on first computation and keep it on corrections, add `GetFinishedPoolGamblerBetsByMatchDate-index` to the template, and document both in `docs/database-model.md`; verify the index excludes pending details and the existing finished index is untouched.

## 2. Backend — grouped feeds, kickoff changes, and backfill

- [x] 2.1 Replace the History response with kickoff groups (kickoff descending, children by match ID, group completed at page boundaries, `movement` from the earliest and latest computed snapshots, null when missing); verify endpoint tests cover a group split by a storage page, a partial group with counts, a legacy child without snapshots, and rejection of an old finished-index token with `INVALID_CURSOR`.
- [x] 2.2 Replace the Timeline response with group-level phases: unsettled groups from the pending index with their computed siblings fetched by kickoff, then finished groups skipping any kickoff with a resultless match; verify tests cover a partial group appearing once, the phase change after the last result, empty storage pages, and existing visibility rules on children and counts.
- [x] 2.3 Add the PoolLayout stream branch for a changed `matchDateTime` that fans out through `GetPoolGamblerScoresByMatch-index` and conditionally updates `matchDateTime` and `computedMatchDateTime` on every detail; verify a postponed pending match regroups in both feeds and a repeated delivery makes no writes.
- [x] 2.4 Add the restartable `BACKFILL_KICKOFF_GROUPS` maintenance job (copy kickoff onto computed details, set the root trend kickoff from the latest completed `RESULT#` receipt) and its operator entry; verify a rerun makes no writes and a mid-tournament pool gets the expected values.
- [x] 2.5 Update `docs/pool-settlement.md` with the baseline rule, trend kickoff, grouped feeds, kickoff-change propagation, the ingestion-schedule operator note, and backfill; verify the settlement, position, restoration, and pagination suites pass with the existing exact-once guarantees intact.

## 3. iOS — grouped contracts and shared presentation

- [x] 3.1 Extend DataBet/DataPool transport, domain, and presentation models for group items, `movement`, and `trendGroup`, deriving pending/numeric/partial/unavailable states; verify decoding tests cover each state, an absent `trendGroup`, and a null `movement`.
- [x] 3.2 Commit canonical vector sources for any introduced or changed trend glyph and derive iOS assets; verify up/down/steady assets share one source and use Material Symbols where available, with no raster extraction or navigation-asset changes.
- [x] 3.3 Compose the shared group header, rail, dividers, and existing matchup/prediction/points rows for History and Timeline, with `TrendIndicator` extended for the qualifier and pending/unavailable labels; verify previews for one to four matches, partial and pending groups, `Your bet` versus `Bet`, and independent child activation against the corrected reference.
- [x] 3.4 Adapt both pagers to whole-group pages, group-level Timeline phases, and the invalid-cursor first-page reload; verify tests cover append order, the partial-to-finished transition on refresh, stale responses not merging, and bounded retry on repeated rejection.
- [x] 3.5 Render trend-group movement with qualifiers and states on the leaderboard and My pools, add English, Spanish, and Spanish (Spain) strings with plural forms, coherent header and row announcements, and placeholders through `isPlaceholder`; verify accessibility tests cover the announcement order, direct child focus, inert placeholders, and large text.
- [x] 3.6 Verify summary-only versus list-only retry, pull-to-refresh, empty and error states, and safe-area reachability on supported iPhone and iPad sizes and themes in the simulator, add only meaningful native tests with explicit waits, and update the Bet and Pool package READMEs with the grouped contracts.

## 4. Android — grouped contracts and shared presentation

- [x] 4.1 Run `adb devices -l`, confirm the device with the user if ambiguous, and explore History, another gambler's Timeline, the leaderboard, and My pools with ARTEMIS, recording verified screens, locators, and waits in the change's verification notes before writing UI tests.
- [x] 4.2 Extend data/bet and data/pool transport, domain, and presentation models for group items, `movement`, and `trendGroup`, grouping by the server's kickoff instant rather than a local `LocalDateTime`; verify decoding and state tests cover DST-colliding labels, an absent `trendGroup`, and a null `movement`.
- [x] 4.3 Derive Android trend assets from the sources committed in 3.2 and compose the shared History/Timeline header, rail, dividers, and production rows with the extended `TrendIndicator`; verify rendered glyphs, reference hierarchy, owner wording, and independent child activation without a navigation-shell change.
- [x] 4.4 Adapt Paging composition to whole-group pages, group-level Timeline phases, and the invalid-cursor first-page reload; verify stable keys, no duplicated headers, correct order, the partial-to-finished transition, and bounded retry.
- [x] 4.5 Render trend-group movement with qualifiers and states on the leaderboard and My pools, add English, Spanish, and Spanish (Spain) strings with plural resources, TalkBack header and row semantics, and placeholders through `isPlaceholder`; verify no decorative focus stops, no filler exposure or remote loading, large text, and reduced-motion behavior.
- [x] 4.6 Verify supported API, theme, window, and text-size combinations and edge-to-edge scroll limits, run the relevant build and focused tests plus ARTEMIS-verified UI tests with dynamic locators and explicit waits, and update the Android Bet and Pool documentation and verification notes with reproducible commands.

## 5. Integration acceptance

- [x] 5.1 On a shared fixture pool, settle two same-kickoff matches separately, then a late result for an older kickoff, then postpone a pending match; verify both apps show pending, then `+n, 1 of 2`, then `2 of 2` from the same baseline on History, Timeline, the leaderboard, and My pools, the older result does not change the trend group, and the postponed match regroups.
- [x] 5.2 Page a History feed whose group spans a storage page and a Timeline whose group changes phase between pages; verify both clients show each group once with full counts, and an old finished token is rejected and recovered with one first-page reload.
- [x] 5.3 Run `openspec validate group-kickoff-rank-movement`, review delivered screenshots against the reference and the accessibility and icon contracts, and reconcile every task with its recorded verification.
