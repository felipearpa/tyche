# Spec Delta

## Purpose

Give Fortuna one trustworthy rank movement for simultaneous fixtures, shared across historical match groups and current pool surfaces, including progressive computation and corrected results.

## ADDED Requirements

### Requirement: Kickoff groups have authoritative identity and membership

The service SHALL identify a kickoff group by the selected pool and the exact scheduled kickoff instant normalized to UTC. Equivalent timestamps with different offsets SHALL identify the same group; different kickoff instants SHALL remain separate even on the same calendar day or when localized labels coincide. Group membership and full match counts SHALL come from the pool's authoritative fixture scope, independently of which bets, rows, or pages a client has loaded. Responses SHALL provide an opaque group identity, kickoff instant, total match count, computed match count, and computation state. A match SHALL count as computed only after its accepted pool settlement has completed, never merely because kickoff passed or a score exists.

#### Scenario: Two fixtures share one kickoff
- **GIVEN** two matches in one pool have the same normalized scheduled instant
- **WHEN** either history feed or standings expose their group
- **THEN** both matches have the same group identity and the authoritative total count is two
- **AND** another pool's fixtures cannot enter that group

#### Scenario: Local labels are not group keys
- **GIVEN** two timestamps represent the same instant using different offsets, and a third represents a different instant with the same local clock label
- **WHEN** the service identifies groups
- **THEN** the equivalent instants share a group and the third does not

#### Scenario: A score is available before settlement completes
- **GIVEN** a group contains two matches and only one has completed pool settlement
- **WHEN** the other match has a score but its settlement is pending
- **THEN** the group reports one computed match out of two

### Requirement: Movement compares chronological group boundaries

For each gambler with sufficient historical evidence, group movement SHALL equal rank immediately before the group minus rank after applying all currently computed matches in that group. The before boundary SHALL include eligible, computed awards from strictly earlier scheduled kickoff groups; the after boundary SHALL additionally include this group's computed awards. Later groups SHALL influence neither boundary. Ranking at both boundaries SHALL use total points descending and gambler identifier ascending, preserving unique positions. Awards and eligibility SHALL follow the existing scoring and late-join rules. Clients SHALL consume the authoritative movement instead of adding visible per-match deltas or points. For identical accepted results, participation evidence, and schedule, the result SHALL be independent of match-result arrival order.

#### Scenario: A group gains and then loses a position
- **GIVEN** a gambler is eighth before a two-match group, fifth after the first computed match, and sixth after both
- **WHEN** both matches have computed
- **THEN** the group's movement is positive two and its rank boundaries are eight and six
- **AND** it is neither negative one from the final match nor positive four from adding movement magnitudes

#### Scenario: Another kickoff is processed between group members
- **GIVEN** A1 and A2 share a kickoff earlier than B, and results arrive in order A1, B, A2
- **WHEN** the published history converges
- **THEN** A's movement compares its chronological boundaries using both A results without B's award
- **AND** B's movement is recalculated using the complete accepted awards of A before B

#### Scenario: Equal totals need a deterministic rank
- **GIVEN** two gamblers have the same total at a group boundary
- **WHEN** their boundary ranks are calculated
- **THEN** their identifiers break the tie in ascending order on every replay

### Requirement: Progressive movement retains the group baseline

After every newly completed match settlement, the service SHALL publish movement for its partial or complete group and any affected later groups. Within a group, computing another member SHALL NOT replace the before boundary with the position after the preceding member. A correction or late result from an earlier kickoff SHALL recalculate the before boundary and dependent after boundary together. A group with no computed matches SHALL report pending movement without a number. A partially computed group SHALL identify its computed/total progress alongside available movement. Zero movement SHALL mean a confirmed equal rank at both boundaries, not missing evidence, pending computation, or failure.

For a known group with zero computed members, the grouped-feed contract SHALL retain group identity, kickoff, publication revision, full count, and computed count zero while returning explicit pending movement without numeric rank boundaries or movement. This SHALL remain distinct from the score contract's pool-level `pending` / `noComputedGroups` state, which selects no group. Pending SHALL NOT be represented as a confirmed zero comparison.

#### Scenario: First and second computations arrive separately
- **GIVEN** the group's before rank is eight
- **WHEN** the first computation produces rank five and the second produces rank six
- **THEN** the first publication exposes positive three with one of two computed
- **AND** the next exposes positive two with two of two computed, retaining before rank eight

#### Scenario: Nothing is computed yet
- **GIVEN** a group has zero computed matches
- **WHEN** the service returns that known group to Timeline
- **THEN** it retains the canonical group identity, kickoff, publication revision, and full count, reports computed count zero, and returns an explicit pending movement state without numeric rank boundaries or movement
- **AND** neither reported match scores nor an elapsed kickoff establish computation or justify a numeric zero, directional arrow, or steady indicator
- **AND** personal History omits the group because it has no computed children

#### Scenario: A pending group obtains its first computed member
- **GIVEN** a two-match group has zero computed members and pending movement
- **WHEN** its first completed pool settlement is coherently published with a trustworthy comparison from rank eight to rank six
- **THEN** the group reports positive two with one of two computed using its captured start-of-group baseline
- **AND** the remaining member retains pending computation and the whole group remains unsettled

#### Scenario: A computed partial group has no net movement
- **GIVEN** one of two matches is computed and its group boundary ranks are equal
- **WHEN** the group is published
- **THEN** it exposes confirmed zero movement with the partial-computation qualifier

### Requirement: Historical recalculation preserves the newest kickoff selection

Late results and accepted result corrections SHALL update the originating group's awards and recalculate that group's movement and all later group comparisons whose inputs change. The newest group for leaderboard and My pools movement SHALL be the greatest scheduled kickoff instant in the pool with at least one computed match. An uncomputed future group SHALL NOT replace it. Processing an older group again SHALL NOT make that group the newest. History, Timeline, leaderboard, and My pools SHALL agree when read for the same gambler, group, and published revision. Historical recalculation SHALL change derived group comparisons without applying an award twice or modifying the retained legacy per-match position snapshots as part of the replay.

#### Scenario: Yesterday's result arrives after today's
- **GIVEN** today's group is the newest group with a computed match
- **WHEN** yesterday's first result arrives late
- **THEN** yesterday's group and affected later movements are recalculated
- **AND** the leaderboard still refers to today's group using its recalculated comparison

#### Scenario: An old result is corrected
- **GIVEN** groups A, B, and C are ordered by scheduled kickoff and C is newest
- **WHEN** A's award changes under an accepted correction
- **THEN** the published revision reflects the corrected boundaries for A and any affected B and C comparisons
- **AND** C remains the newest group and existing settlement adjusts current totals exactly once

#### Scenario: A future group is visible in Timeline
- **GIVEN** the newest computed group is today and tomorrow's group has no computed match
- **WHEN** Timeline includes tomorrow under existing visibility rules
- **THEN** tomorrow's movement is pending and today's group remains the standings movement source

#### Scenario: A newer kickoff passes without any computation
- **GIVEN** an older group has a computed member and a newer group's kickoff has passed but all its settlements remain pending
- **WHEN** Timeline and standings read the same published revision, even if the newer group's match scores are available
- **THEN** Timeline reports pending movement for the newer group while leaderboard and My pools retain the older eligible group's movement
- **AND** only after a first completed settlement is published for the newer group does it become the standings movement source

#### Scenario: No group has computed yet
- **GIVEN** the service supports grouped movement but no group in the pool has a computed match
- **WHEN** leaderboard or My pools requests movement
- **THEN** the response explicitly reports pending with reason noComputedGroups and no selected group or numeric rank boundaries
- **AND** this successful pending response is distinct from an invalid missing required object, explicitly unavailable history, or a failed read

### Requirement: Participation changes do not fabricate historical movement

Each group's rank comparisons SHALL use a fixed comparison cohort of the active gamblers and their membership lifetimes captured at the first successful membership-revision-fenced cohort commit during the first result's admission/capture phase, before its settlement proceeds. A failed capture SHALL retry against a coherent roster; after successful capture, retries SHALL retain that recorded cohort rather than rescan it as a new baseline. Both boundaries and every later replay of that group SHALL use that cohort and its preserved award-entitlement evidence. Later joins, removals, and rejoin restoration SHALL NOT retroactively replace it with the current member list. A late join after the cohort is captured SHALL retain normal current-score eligibility but have unavailable movement for that group. Rejoining SHALL preserve retained completed history while a new membership SHALL NOT borrow an incompatible old membership's baseline. Restored match awards SHALL appear once in any eligible later comparison. A correction SHALL be able to recalculate supported historical comparisons for an absent gambler without crediting a removed membership. Missing or ambiguous entitlement evidence SHALL produce unavailable movement for affected comparisons rather than an invented zero or baseline.

#### Scenario: A gambler joins after a group begins
- **GIVEN** a group has captured its comparison cohort and computed one match
- **WHEN** a new gambler joins before another member match computes
- **THEN** the existing cohort's historical comparison remains consistent
- **AND** the new gambler's current rank and score follow existing rules while this group's movement is unavailable for that membership

#### Scenario: A gambler rejoins with retained bets
- **GIVEN** completed groups and their original membership comparisons exist before a gambler leaves and rejoins
- **WHEN** restoration completes
- **THEN** those historical group cohorts and retained legacy positions are preserved
- **AND** future eligible comparisons include restored awards once and use the new membership context
- **AND** an in-progress group spanning incompatible membership contexts does not invent one combined baseline

#### Scenario: A result is corrected while a participant is absent
- **GIVEN** a historical group's preserved evidence includes a gambler who has since left
- **WHEN** an accepted correction changes an eligible historical award
- **THEN** affected derived group movements are recalculated using the recorded cohort and prediction eligibility
- **AND** no points are written to a removed current membership

### Requirement: Schedule changes update group identity consistently

A corrected scheduled kickoff SHALL move its match to the group for the corrected instant, update affected group counts and phase state, and recalculate comparisons from the earliest affected kickoff onward. Empty groups SHALL cease to appear in feeds while their captured cohort evidence remains retained for a possible later return to that instant. The newest computed-group selection SHALL be reevaluated from the resulting schedule. Prior movement SHALL NOT be relabeled as belonging to a new timestamp without recalculation. Existing comparison cohorts SHALL remain fixed; a newly formed computed group without an existing cohort SHALL capture its cohort through the first successful fenced capture of a durably admitted, versioned schedule-change operation. Schedule replay SHALL use the accepted schedule revision, and repeated schedule delivery SHALL NOT replace an already captured cohort. A moved match with insufficient preserved entitlement evidence SHALL produce an explicit unavailable comparison rather than speculative history.

#### Scenario: A computed match moves to another kickoff
- **GIVEN** a computed match belongs to group A
- **WHEN** its authoritative kickoff is corrected to group B's instant
- **THEN** the next published revision places it only in B, adjusts A and B counts, and recalculates affected comparisons
- **AND** old continuations cannot silently merge the incompatible group memberships

### Requirement: Published group results are coherent and recoverable

The service SHALL expose a published revision for grouped history and newest-group movement. Counts, computation state, rank boundaries, movement, fixture membership, and displayed match settlement data within that revision SHALL agree. An unfinished replay SHALL NOT expose a mixture of new and old group results as current. Reads SHALL either return the last coherent publication explicitly marked updating or return an explicit unavailable/recoverable state. Publication readiness SHALL require complete output coverage, including explicitly classified unavailable comparisons where evidence is genuinely absent. Proven missing historical evidence SHALL NOT indefinitely block settlement or publication; transient failed reads and incomplete capture SHALL remain retryable instead of being mislabeled historical absence. A newer accepted update SHALL prevent an older replay from replacing it. Duplicate deliveries, retries, or repair without changed inputs SHALL neither count a match twice nor reset the group baseline. Current score and position SHALL remain independently available under their existing consistency contract; consumers SHALL NOT derive grouped movement by subtracting a historical boundary from an unrelated current rank.

#### Scenario: Replay fails after updating some staged groups
- **GIVEN** a correction requires several historical groups to be rebuilt
- **WHEN** replay stops before all required results are ready
- **THEN** clients cannot read those partial staged results as the current publication
- **AND** existing usable publication is explicitly updating or movement is unavailable while recovery resumes

#### Scenario: Duplicate settlement or reconciliation
- **GIVEN** a match version already contributes to a group
- **WHEN** the same input is retried or reconciled without changing accepted evidence
- **THEN** counts, awards, and movement remain unchanged

#### Scenario: Current rank changes independently
- **GIVEN** a membership change updates the current leaderboard rank after the newest group comparison was published
- **WHEN** the row renders
- **THEN** its current rank remains current and its movement remains the authoritative group comparison
- **AND** it does not manufacture a new group delta from the unrelated current rank

### Requirement: Historical adoption is truthful under a breaking contract cutover

The affected finished/Timeline APIs SHALL replace their flat per-match responses with grouped contracts. Score responses SHALL require an explicit `groupMovement` state, retain `position` as the current rank, and retire `beforePosition` from the contract. Old affected response modes, cursor adapters, last-match trend fallbacks, and client capability negotiation SHALL NOT be maintained. A missing or malformed required movement state SHALL be a recoverable response failure, not successful unavailable data. Existing stored historical snapshots SHALL be retained as evidence without an obligation to keep their old consumers or movement writes; chronological replay SHALL NOT overwrite them as recovered group history. Older grouped history SHALL be reconstructed only where authoritative schedule, accepted awards, cohort, and entitlement evidence support it. Unrecoverable groups SHALL retain available matches, points, and dates with unavailable movement; subsequent groups SHALL remain unavailable where their boundaries depend on unsupported evidence. Availability SHALL be reported per comparison rather than implied by a successful backfill job. A failed source read SHALL remain retryable and SHALL NOT be classified as proven historical absence.

#### Scenario: Older snapshots cannot establish the cohort
- **GIVEN** legacy bets retain per-match positions but no trustworthy group participation evidence
- **WHEN** grouped history is initialized
- **THEN** those positions are not summed or treated as a recovered event journal
- **AND** the matches remain visible with unavailable group movement until valid evidence supports reconstruction

#### Scenario: A pre-cutover cursor reaches a replaced feed
- **GIVEN** a caller holds an old finished per-match or Timeline phase continuation
- **WHEN** it is submitted after the grouped contract cutover
- **THEN** the service rejects it with the explicit invalid-cursor restart response
- **AND** a fresh request returns the new grouped shape for updated clients without an old-client compatibility mode

#### Scenario: A required movement state is missing
- **GIVEN** an updated score or grouped-feed response omits or malforms its required movement state
- **WHEN** a client decodes it
- **THEN** it exposes recoverable request failure without treating missing data as a confirmed unavailable historical comparison or inferring a value from old fields

#### Scenario: Backfill overlaps a new correction
- **GIVEN** initialization is processing an older accepted result version
- **WHEN** a newer correction is accepted
- **THEN** initialization cannot overwrite the newer published movement or award
- **AND** unresolved migration evidence remains explicit
