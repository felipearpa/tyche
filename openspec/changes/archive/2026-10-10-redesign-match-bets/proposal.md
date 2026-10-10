# Proposal

## Why

The match-bets screen still uses an older header and participant layout that does not match Fortuna's redesigned History, Timeline, and Scores screens. Participants also need to appear in descending order of their total points in the selected pool so the pool leader is first, regardless of the points earned on this particular match. Pools are expected to contain thousands of participants with many simultaneous viewers, so the ranked feed needs to return complete participant rows without fetching each participant's bet after reading the leaderboard.

## What Changes

- Redesign the iOS and Android match-bets destination around three supplied references: [hidden bets](reference/match-bets-hidden.png), [revealed bets awaiting results](reference/match-bets-revealed.png), and [computed result](reference/match-bets-finished.png). Preserve originals and repair missing navigation/status/lock icons in reference copies; [reference notes](reference/README.md) distinguish visual examples from the behavior specified here.
- Use a shared match-header hierarchy, circular account avatars, readable participant names, explicit missing predictions, thin separators, and History-style points pills after computation. Retain the existing Score title and Back/Home destinations.
- Keep the current prediction-visibility policy, including its ten-minute pre-kickoff lock. Keep existing match-detail entry actions enabled for loaded upcoming matches and show the hidden state after navigation. Give that state a centered start-time pill, teams around VS, a neutral lock illustration, concise hidden-bets copy, and the signed-in gambler's own prediction. The latest reference supersedes the earlier idea of retaining the old warning wording.
- Describe the revealed/uncomputed state truthfully as Bets closed using the existing data. The reference's In progress label is not evidence of live match status; adding a live-status data source is outside this change.
- Store a separately named copy of current total pool points on each match-bet detail row and maintain it from the authoritative membership total. Order revealed and computed lists by those propagated totals descending across the complete paged feed; retain gambler ID ascending for ties and unavailable totals last. Keep participants without a prediction and the current user in their ranked positions. Displayed points pills continue to represent this match's award; no total-points column is added.
- Add a dedicated pool-and-match ranking index that returns the participant identity, prediction, match award, and ranking data together, with native continuation through ties. Remove the leaderboard-page-plus-batch-bet composition from the planned request path. Preserve the existing computation index and legacy endpoint mode.
- Refactor result processing into a durable per-pool command on the existing FIFO queue. Its consumer computes bets and finalizes positions in one ordered, bounded, resumable operation, with checkpoints on existing records rather than a separate scoring-job table or service. Keep domain scoring pure, orchestration in application/event handlers, and repositories limited to data access.
- Make settlement, corrections, and position updates safe under partial failures, duplicate/old delivery, and membership changes. Read authoritative membership totals for position calculation; do not use eventually consistent indexes as completion barriers. Preserve scoring formulas and existing late-join rules, and reconcile matches materialized after a result.
- Establish retry/record-age limits, stream batch isolation, full-event failure retention, FIFO dead-letter handling, alarms, and replay procedures before activating the refactored flow. Add failure-path tests before changing settlement behavior and update the manual correction procedure to use the same ordered, versioned write contract.
- Add durable, versioned background propagation to every affected match row, including historical matches, plus safe initialization, member removal/rejoin handling, reconciliation, and a resumable existing-data migration. Accept temporary ranking and participant-projection lag during propagation, then converge to current standings; requester authorization and the prediction-visibility gate remain authoritative request checks.
- Validate both concurrent reads and the additional score-update writes at the expected scale. Shared response caching and frozen standings snapshots are outside this selected approach.
- Apply the established accessibility, localization, native press-feedback, theme, safe-area/inset, refresh/retry, and production-component placeholder conventions. The own row remains informational; other participants retain Timeline navigation.

## Capabilities

### New Capabilities

- `match-bets`: Match header, hidden/revealed/computed presentation, own-prediction ownership, participant rows, recoverable scoring and position finalization, current-pool-total projection and ordering across pagination, failure recovery and migration compatibility, and accessible localized behavior on iOS and Android.

### Modified Capabilities

None. This change consumes the existing History, leaderboard, avatar, placeholder, and platform-surface conventions without changing their requirements or unrelated destinations.

## Impact

- Backend: additional match-bet projection fields and a new ranking index; membership generation/score versions, result/operation versions, pool-root checkpoints, and small settlement receipts in the existing Pool table; result-handler and FIFO-consumer orchestration, atomic settlement/position writes, authoritative position reads, stream/queue workers, lifecycle propagation and repair; direct ranked endpoint reads and opaque pagination; failure destinations, migration, observability, and rollback. Scoring formulas, match-award meanings, original index keys, and existing late-join rules are preserved while scoring orchestration changes. Infrastructure ownership for the externally managed Pool table/indexes must be established during implementation.
- iOS: `Bet/Match` presentation and state, `DataBet` request wiring, authenticated-account context, shared Account avatar and UI interaction/placeholder components, and localized resources.
- Android: the corresponding `bet/match`, `data/bet`, DI/navigation context, shared Account/UI dependencies, and English/Spanish resources.
- Planning assets: three supplied original screenshots, corrected reference images, reference provenance/edit prompts, design decisions, and platform-grouped implementation/verification tasks. This proposal does not implement application or test code.
