# Design

## Context

See [proposal.md](proposal.md) for the problem and scope. The current backend serializes operations per pool through SQS FIFO, but processes results individually in arrival order. `PositionPolicy` replaces membership `beforePosition` on each settlement or correction. Per-match position snapshots are mutable and are not exposed by `PoolGamblerBetResponse`; corrections preserve `computedDateTime` while changing the associated position context. Finished history uses that computation timestamp as its index order.

iOS and Android already share the production History row and points summary with another gambler's Timeline. Both leaderboard and My pools derive movement from the same `beforePosition - position` client model. Timeline currently concatenates unsettled and finished per-match feeds. These seams permit shared presentation, but current data cannot support correct grouping through a client-only change.

Persistence follows [docs/database-model.md](../../../docs/database-model.md) and [docs/pool-settlement.md](../../../docs/pool-settlement.md): separate bounded-context tables, a resumable per-pool operation, membership-generation and revision fences, versioned accepted results, and exact-once restoration. New historical projections must coexist with these guarantees.

## Goals / Non-Goals

**Goals:**

- Make group movement deterministic for the same accepted results, schedule, and recorded participation evidence, even if results arrive in a different order.
- Publish coherent counts, match computation state, and movements while permitting progressive updates and bounded, retryable rebuilding.
- Keep current standing and historical group comparison separate so membership changes do not manufacture a match trend.
- Replace the affected client/server contracts directly, with required grouped data and truthful historical availability; support for older applications is not required.

**Non-Goals:**

- Change point formulas, prediction lock/reveal rules, rank tie breaks, or current membership restoration policy.
- Replace the navigation shell, copy sample match values into production, introduce live polling, or use raster icons as app assets.
- Recover a historical roster or prediction entitlement from evidence that was never retained.

## Decisions

### 1. Canonical group identity is pool plus kickoff instant

Normalize authoritative scheduled timestamps to UTC without rounding or a processing-time tolerance. Derive an opaque stable group ID from pool and normalized instant, and send that ID to clients. Never group on formatted date/time: Android currently converts transport timestamps to local `LocalDateTime`, and DST can make different instants display alike.

Group membership comes from the pool's fixture scope. Counts include its full group, while child visibility follows each feed's existing authorization and prediction gate. History returns computed children only; a partial group can therefore show one child with `1 of 2 computed`. Timeline returns eligible visible children, including computed children in a partial group. Count data does not authorize revealing a hidden prediction.

The newest movement group is `max(kickoffInstant)` among groups with at least one completed, accepted pool settlement. Groups with zero computed matches are never candidates, even if their kickoff has passed or their rows contain reported match scores. While a newer group is wholly pending, leaderboard and My pools retain the newest eligible computed group's movement; if none exists, they show the explicit `pending` / `noComputedGroups` state. An older correction updates the values attached to that newest group without changing the pointer to the corrected older group.

**Alternative:** a short time window around worker completion is unstable under queue delays, retries, and corrections and does not express simultaneous fixtures.

### 2. Compute chronological group comparisons from preserved sources

For each group G and gambler in its comparison cohort, calculate:

```text
beforePoints(G) = eligible accepted awards at kickoffs strictly before G
afterPoints(G)  = beforePoints(G) + eligible accepted awards at G

beforeRank = rank(beforePoints for G's cohort)
afterRank  = rank(afterPoints for that same cohort)
movement   = beforeRank - afterRank
```

Use existing total-descending / gambler-ID-ascending ranks. Uncomputed results contribute no accepted award yet, but remain pending in group progress. A known no-bet/late-join zero is distinct from missing source evidence. Because every cohort member's points affect everyone else's rank, an unresolved source that could change the comparison makes the affected cohort comparison unavailable; do not mark only the source owner's rank unavailable while claiming the others are exact.

After each completed member settlement, the current group result is provisional until all members compute. For ranks `8 → 5 → 6`, publish `+3, 1/2` and then `+2, 2/2`. The starting rank does not reset to five. A late result from an earlier group legitimately changes both boundary calculations; the baseline is stable against other members of the same group, not permanently immune to historical correction.

Recalculate the changed kickoff and dependent later groups on an accepted earlier result or correction. Use result versions accepted for this pool's projection; do not read an arbitrary newer PoolLayout result while replaying an older admitted operation. Older corrections reevaluate immutable prediction/eligibility sources even if the original bettor has since left. This is a derived history operation and never credits a removed membership.

**Alternatives:** summing mutable per-match snapshots loses earlier movement after corrections or repair; comparing actual position before A1 and after A2 includes unrelated B if processing order is A1, B, A2. Neither defines the requested chronological history.

### 3. Freeze comparison cohorts and entitlement evidence

During the first result's admission/capture phase, before its settlement proceeds, capture active gambler IDs and membership generations under a coherent `membershipRevision`. The precise cohort boundary is the first successful fenced capture commit, not an unrecorded wall-clock admission instant. A concurrent membership change before that commit restarts capture; after it succeeds, all retries retain the same committed cohort. The cohort remains fixed for both rank boundaries and all later replay of that group. A join after capture follows normal score eligibility but has unavailable movement for that group. Removal does not erase an earlier cohort. Rejoin creates a new membership context for future groups, preserves earlier completed history, and cannot borrow an incompatible baseline for a partially computed group.

Keep historical contribution identity separate from the mutable detail's current membership binding. A source records original entitlement identity, match ID, prediction or explicit no-bet, eligibility policy, and supported schedule identity. Restoration preserves the origin when rebinding retained awards; a newly created detail after an absence has a new origin. Each cohort member references an immutable entitlement manifest for its cumulative comparison, including eligible retained awards exactly once. Late-join-zero and held/unavailable are explicit policies, not inferred from whether a current detail exists.

Store manifests as bounded records/references, sharing immutable source sets where possible, rather than copying an unbounded array into a group or pool item. Capture sources prospectively as predictions become fixed and before membership removal/rebinding can destroy their origin. Only finalize a cohort/manifest after strong reads and a successful membership revision fence; interrupted capture resumes idempotently. Existing operation serialization protects settlement/restoration, while the membership revision guard handles joins and removals outside that operation.

This freezes comparison population at the successful first-admission capture boundary, not at an inferred historical kickoff roster. It is an intentional deterministic rule using evidence available to the service. Before and after always use the same cohort, so joining/restoring itself is not represented as match movement. The UI's current rank can differ from a historical after-rank if membership has since changed; the new movement object keeps those meanings separate.

**Alternative:** rebuilding old groups against today's members silently rewrites history on every join/removal and conflicts with retained history expectations. Inferring old membership solely from a present-day generation or ULID is not adequate evidence.

### 4. Publish group projections and replace the movement contract

Extend the Pool bounded context with additive entities for:

| Record | Purpose |
| --- | --- |
| Historical entitlement/source | Immutable prediction, source origin, and eligibility facts surviving removal/restoration |
| Accepted result/award input | Monotonic result version, pool acceptance revision, and evaluated historical contribution |
| Group cohort and manifest references | Fixed gambler/generation comparison population and its source bindings |
| Revision group summary/member comparison | Kickoff, member counts, phase, rank boundaries, movement availability, and reason |
| Revision feed children | Match data and settlement state consistent with the group's published revision |
| Pool publication/checkpoint | Staged replay progress, published revision, newest-group pointer, readiness, and updating state |

Use dedicated keys and attribute names, such as `kickoffInstant` and `points`, which cannot accidentally enter indexes gated by `score`, `gamblerUsername`, `matchDateTime`, or `computedDateTime`. Do not put new projection records in queries intended for `GAMBLER#` membership rows. Update persistence documentation with the chosen key/access patterns during implementation.

Integrate a resumable projection stage with pool operations after scoring and position calculation. Persist the changed-input/dirty-range checkpoint durably before operation completion so a crash cannot acknowledge a correction without scheduling its history repair. Stage the affected suffix of groups, reusing unchanged immutable prefix data through revision manifests. Verify complete output coverage and accepted-input version consistency before conditionally publishing the new revision and newest pointer with operation completion. Complete output includes explicitly unavailable comparisons for proven missing or held legacy evidence; such classifications must not strand the existing FIFO operation. A transient source-read failure or incomplete capture remains retryable and must not be converted into historical absence. Older workers and duplicate operations cannot publish over a newer accepted input.

Keep replay bounded per invocation and checkpoint at group/member/source boundaries. Store cumulative checkpoints keyed by cohort/manifest and accepted-input version; invalidate dependent checkpoints from the earliest changed kickoff. Reads never replay history or scan the entire pool. Repositories remain data access only; the application layer owns orchestration and queue work, using idiomatic immutable F# state.

During rebuilding, serve the last complete publication with `isUpdating`, or an explicit unavailable/recoverable state if none exists. Counts and child settlement values must come from that publication too: a fresh live detail must not make an old group summary appear final. Current totals and positions retain their existing independently available consistency behavior.

Make `groupMovement` a required discriminated state in the replacement score contract. A selected-group state carries group ID, kickoff, publication revision, before/after rank when available, signed movement when available, availability, computed/total counts, and updating state. A pool with no computed group returns `state: pending`, `reason: noComputedGroups`, its publication revision, and no selected group, counts, or numeric boundaries; clients render pending without inventing a 0-of-0 group. A missing or malformed required object is a recoverable contract/read failure, not a successful unavailable or pending result. Preserve pending versus explicitly unavailable versus retryable failure on both score surfaces. Remove membership `beforePosition` from the score response and remove last-match trend code/writes once all affected consumers have moved to the group contract. Keep `position` and authoritative current score, together with their existing concurrency and settlement safeguards. My pools and leaderboard consume the same required group state; the drawer still uses current rank and points. Existing stored per-match snapshots are retained as historical evidence; this change does not require deleting them, extending their old API semantics, or maintaining them solely for an old client.

**Alternative:** using a group baseline with the current `position` still gives the wrong delta after a membership change. The separate group before/after pair is needed for the agreed historical semantics even without backward compatibility. Keeping two API shapes or a last-match fallback would add unneeded maintenance and is rejected.

### 5. Introduce grouped feeds with bounded group segments

Replace the finished and Timeline endpoint contracts in place with the grouped shape; do not keep a flat per-match mode or an old response adapter. Keep the useful `items` and optional opaque `next` envelope, but change the item shape to groups. The new cursor format has an identifiable version for validation, not for serving both old and new response schemas. Each item is a group or contiguous segment containing authoritative metadata plus visible children; the UI normalizes segments into a single logical section keyed by group ID.

- History: groups by kickoff descending; only computed children.
- Timeline: groups with any uncomputed member first, kickoff ascending; complete groups next, kickoff descending. A partial group's computed child stays with its pending siblings rather than being duplicated in the finished phase.
- Children: match ID ascending for a stable order independent of result arrival.
- Large groups: bound response size and continue members of the same group before moving to the next. Full count and movement never depend on loaded child count.

Cursors include scope, endpoint, phase, ordering version, publication revision, and complete group/member storage boundaries. Prior flat per-match and old Timeline phase tokens are invalid for the replacement endpoints, even when they contain complete old index keys; reject them with `INVALID_CURSOR` and require a fresh request from an updated client. Unrelated endpoints and their cursors stay outside this cutover. For this implementation, reject an append from a no-longer-current publication with the existing explicit `INVALID_CURSOR` first-page restart contract; retain no unbounded historical cursor service. A single page read captures a publication at start and verifies it before returning, or uses immutable revision reads known to remain available throughout the read. Never silently advance to a newer publication.

Clients handle that explicit revision restart by replacing the feed from page one, preserving usable visible content with refresh feedback until replacement succeeds. Prevent stale appends from merging into the new request generation. Failed recovery remains bounded and user-retryable; an invalid cursor must not create an automatic retry loop. Current summary data remains independent of feed reset, as it already is for row retry.

**Alternative:** client grouping of legacy pages fails when equal-kickoff matches are nonadjacent in computation order, and a page of two children cannot establish whether the group actually contains two or four matches.

### 6. Compose one shared group presentation on each platform

Use [reference/history-grouped-icons.png](reference/history-grouped-icons.png) as the visual base and preserve [reference/history-original.png](reference/history-original.png) for provenance. The corrected image replaces blank blocks with up/down arrows, a steady dash, swap arrows, trophy, sports-score flag, and history clock. It is a raster design reference with minor generation drift; the written contract controls behavior and production sizing. Sample totals and match scores are illustrative.

Refactor the shared History row into reusable matchup/prediction/points content plus group context. A scrolling group header shows localized date, time and pluralized match count, with a trailing trend pill. Keep the subdued vertical rail and internal dividers. A single completed match can omit the count, matching the image. A partial group adds a localized progress qualifier such as `1 of 2 computed`; pending, unavailable, and updating are distinguishable from steady movement.

When a Timeline group's authoritative computed count is zero, retain its date/time and full match count but show and announce pending movement in the header without a directional arrow, numeric movement, or steady indicator. Every eligible visible child shows its available match score or neutral dashes and `Points pending`; a reported score alone does not establish computation or a final result. This is a known pending group, not a loading placeholder or a numeric zero. On an ordinary refresh after the first member's accepted settlement is coherently published, show available movement from the group's starting rank with progress such as `1 of 2 computed`, while only the computed child adopts its final score and award. The group stays unsettled until every member computes. Personal History continues to omit wholly uncomputed groups because it contains computed children only.

Group headers are not match actions and are not sticky. Each child retains its existing match destination and platform press feedback. Timeline shares this composition and passes selected-gambler ownership, retaining `Bet`; personal History retains `Your bet`. Per-child final/pending scores and points continue using existing settlement evidence. Keep the authoritative earned-points summaries and their independent refresh/retry behavior.

The group header announces date/time, count, progress, and movement once. Child rows retain date/time in their consolidated accessibility context for direct focus, followed by teams, result, owner prediction, and award/pending state. Decorative rail, dividers, icons, and pill backgrounds add no focus stops. Support English, Spanish, Spanish (Spain) where already supported, plural forms, narrow/resized windows, large text, dark/light themes, and existing platform safe areas.

Use the existing shared `TrendIndicator` for direction, magnitude, and semantic colors, extending focused inputs only as necessary. Any introduced or changed app glyph must prefer an accurate Material Symbol or allow a custom symbol without justification; both platforms derive from one committed canonical vector. The mockup's shell glyph repairs communicate intent and do not require a navigation redesign or raster asset extraction. OS-rendered controls are outside that asset requirement.

Model-backed group headers and rows use their production component populated with a placeholder model and the shared treatment selected by `isPlaceholder`. Placeholder behavior can suppress remote loads, navigation, and accessibility exposure, but cannot substitute a separate skeleton layout.

### 7. Handle schedule changes as a projection input

Admit a durable, versioned schedule-change operation independently of result admission, with an immutable accepted schedule snapshot; this also covers an already-settled match whose result receipt is completed. That operation updates canonical kickoff/group membership and invalidates replay from the earlier of old and new kickoff. Rebuild counts, visible phase ordering, chronological boundaries, and newest-group selection together. Remove empty old groups from feed output while retaining their captured cohort metadata. Preserve any existing destination cohort; when the destination has no recorded cohort and now contains a computed match, capture it at the first successful fenced capture commit of this schedule-change admission. A match moved back to a previously empty group reuses that group's retained cohort. Retries neither replace the cohort nor read an unaccepted newer schedule version. Keep original source identity and recorded eligibility; a schedule edit must not invent retroactive prediction rights. When the available source manifest cannot support the moved contribution, mark the affected comparison unavailable until evidence is repaired.

Schedule changes, added/removed fixtures in the pool scope, and membership/source events must all produce durable projection invalidations. Do not assume only a new result version can change the group view. Supported fixture removal changes group membership/counts under existing scoring policy; it does not introduce a new cancellation/scoring policy.

## Risks / Trade-offs

- [Chronological replay is more work than last-event subtraction] → Store source evidence, bounded checkpoints, and staged revision outputs; never calculate from page reads. Measure large-pool rebuild time and storage before activation.
- [Membership history is incomplete in legacy data] → Preserve visible history and report unavailable movement. Capture future evidence rather than claiming a complete historical backfill.
- [A newer score can be visible before group replay finishes] → Keep current totals independent and mark grouped movement updating; never subtract mismatched revisions.
- [A partial group may remain partial for a postponed match] → Continue publishing its available movement and progress; no guessed completion timeout.
- [A frequently changing publication invalidates an append] → Recover with one bounded first-page replacement, preserve content during it, and expose retry on repeated failure.
- [Shared presentation changes affect both owners] → Verify personal and selected-gambler wording, independent summaries, per-row actions, and phase transitions together.
- [A generated reference has small raster differences] → Treat its grouping and icon meaning as the visual direction, retaining native theme, typography, accessible layout, and vector assets.

## Migration Plan

1. Prepare capture, source identity preservation, operation/checkpoint handling, and additive storage before emitting grouped data. Drain accepted active/queued work or explicitly migrate its receipts and checkpoints before changing incompatible worker-state formats; dropping old-client support must not lose accepted work or score it twice. Initialize required source origins safely with existing membership-generation fences; missing metadata is not permission to bypass them.
2. Audit legacy source awards against authoritative membership totals and retained details. Reconstruct a historical cohort only with independent trustworthy participation evidence. Existing operation receipts and overwritten position pairs alone are insufficient. Record availability by comparison.
3. Capture future groups prospectively. A future group's verified source manifest can support its opening totals even when older groups' own cohorts are unavailable; older cohort absence alone does not poison all future comparisons. Unknown award/eligibility evidence that affects its boundaries still blocks that comparison.
4. Build and verify coherent grouped publications, replacement endpoints, explicit cursor recovery, and the required score movement state. Publication readiness still governs data correctness; it is not a client compatibility/capability mode. Proven unknown historical comparisons publish explicit unavailable states, while incomplete initialization and transient failures remain recoverable.
5. Cut over backend and both updated clients as one coordinated contract release. Remove old affected response modes, old affected cursor adapters, optional capability probing, and obsolete last-match movement consumers/writes. Older application versions are not supported by this release; no dual-running window is required.
6. If rollback is necessary, restore a matched backend/client release or temporarily disable the affected entry points during repair. There is no requirement to decode the replaced contract's cursors across this breaking cutover or rollback; invalidate them and restart. Preserve accepted source evidence, authoritative scores, and membership/settlement integrity for recovery. Unrelated endpoint contracts are not changed by this plan.

Verification will cover deterministic same-time groups of one through four matches, mixed gains/losses, ties, partial computation, older interleaved results and corrections, duplicate/repaired work, schedule moves, membership transitions, unknown legacy evidence, large-group paging, and publication-change recovery. Before authoring mobile test code, explore the relevant app paths with ARTEMIS and record observed actions; use verified dynamic locators and explicit waits, with verified coordinate fallback where the chosen framework supports it. No application code or executable tests are authored by this planning change.
