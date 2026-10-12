# Proposal

## Why

Fortuna currently replaces rank movement after each computed match, so simultaneous fixtures leave the leaderboard showing only the last match's effect. History and another gambler's Timeline need one understandable movement for all matches sharing a kickoff, including progressive settlement and corrected historical results.

## What Changes

- Introduce authoritative kickoff groups scoped to a pool and exact normalized scheduled instant, with match counts, computation progress, historical rank boundaries, and movement.
- Calculate movement from the rank before a chronological kickoff group to the rank after its currently computed matches. Update progressively without resetting the group's baseline for each match; distinguish pending, partial, complete, and unavailable movement.
- Before any match in a Timeline group is computed, show pending movement without a directional arrow, numeric movement, or steady indicator; keep every visible child's points pending with its available match score or dashes. Begin progressive movement after the first completed settlement. A wholly uncomputed group does not replace the leaderboard/My pools movement source; if the pool has no computed group, standings movement is pending too.
- Recalculate the affected group and subsequent historical movements after late results, corrections, or schedule changes. Keep the leaderboard attached to the newest kickoff group with at least one computed match, rather than the most recently processed result.
- Group personal History and selected-gambler Timeline on iOS and Android using a shared scrolling date/time/count/trend header, a vertical group rail, and individual match result, prediction, and points rows. Preserve individual match activation and authoritative pool-points summaries.
- **BREAKING**: Require authoritative `groupMovement` on pool-score responses for leaderboard and My pools, keep `position` as the current rank, and retire `beforePosition` from the score API and obsolete last-match trend maintenance.
- **BREAKING**: Replace the affected History/Timeline response shapes with grouped feeds and revision-aware continuations. Reject prior per-match/phase cursors with an explicit restart error. Ship the backend and mobile contract change together without old endpoint modes, compatibility adapters, optional contract probing, or support for older clients.
- Save the supplied design base and an icon-corrected reference in `reference/`. Replace its blank blocks with recognizable trend/navigation glyphs; use the corrected image for hierarchy, with app-rendered assets on both platforms derived from the same committed canonical vector sources.
- Reconstruct older grouped movements only where the authoritative awards and historical participation evidence support them; expose unavailable movement where history cannot be reconstructed truthfully.

## Capabilities

### New Capabilities

- `kickoff-group-standings`: Canonical group identity, chronological rank computation, progressive settlement, historical recalculation, newest-group selection, publication consistency, and truthful migration.

### Modified Capabilities

- `bet-history`: Replace repeated per-match dates with shared kickoff-group headers and movement while retaining the points summary, match actions, loading, recovery, localization, and accessibility.
- `gambler-timeline`: Share the grouped History presentation with selected-gambler ownership and group-level unsettled/finished placement while preserving prediction visibility and per-match pending states.
- `pool-leaderboard`: Consume authoritative newest-kickoff-group movement, including partial and unavailable states, instead of deriving it from the last processed match.
- `pool-score-list`: Align My pools movement and announcements with the same newest-group result without redesigning the pool list.
- `backend-pagination`: Replace affected feed ordering and cursor contracts with grouped continuations and phase transitions, complete group metadata, and explicit revision-change recovery.

## Impact

- Backend: F# Pool domain and operation orchestration, additive DynamoDB group/history projections and indexes, result/correction/schedule and membership integration, Lambda/HttpApi response contracts, resumable migration/rebuild tooling, and persistence documentation.
- iOS: DataBet/DataPool contracts, shared History/Timeline grouping and paging, group presentation, shared trend inputs, localized accessibility, and canonical asset derivatives.
- Android: data/bet and data/pool contracts, shared History/Timeline grouping and paging, group presentation, shared trend inputs, localized accessibility, and canonical asset derivatives.
- Existing scoring formulas, prediction visibility, settlement idempotency, membership-generation fences, and rejoin restoration guarantees remain in force. Stored historical snapshots are retained as evidence, not maintained as an old-client contract or reinterpreted as a reliable event journal. Current position calculation and its transactional safeguards continue; only obsolete last-match trend consumers/writes are removed.
- No deployment or application implementation is performed by this proposal. The backend contract and safe publication/migration path must precede mobile activation.
