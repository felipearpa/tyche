# Spec Delta

## MODIFIED Requirements

### Requirement: Traversal preserves query behavior and retry boundaries

For unchanged data and a fixed query scope, following valid continuations to completion SHALL visit every matching item once in its endpoint's defined order. Repeating a request with the same valid continuation SHALL reproduce its boundary rather than advance through hidden server state. Unaffected queries SHALL preserve their existing filters, ascending/descending order, index convergence requirements, and tie behavior without claiming a frozen snapshot. The replacement History and Timeline contracts SHALL instead use their specified kickoff-group and child order and a coherent published revision. An append whose publication is no longer current SHALL fail explicitly and require a first-page restart, rather than silently follow changed data. Authorization and child visibility rules SHALL remain enforced for every request.

#### Scenario: Equal index values span several pages
- **GIVEN** unchanged matching records share a score, timestamp, or username across at least three storage pages
- **WHEN** all valid continuations are followed
- **THEN** no matching record is skipped or duplicated because the cursor lost its table or index keys
- **AND** unaffected endpoints retain their existing tie behavior while grouped History and Timeline use their specified deterministic group/child order

#### Scenario: A next-page request is retried
- **GIVEN** a page request failed transiently and its scope, underlying data, and any required publication revision remain unchanged
- **WHEN** the caller retries with the same token
- **THEN** it resumes the same storage boundary and returns the same page contents

#### Scenario: Data changes between requests
- **GIVEN** scores, names, dates, membership, index visibility, or the grouped publication changes during traversal
- **WHEN** the caller refreshes without a continuation
- **THEN** a new traversal begins under that endpoint's current query and consistency rules
- **AND** grouped feeds use one current coherent publication while unaffected endpoints acquire no additional snapshot guarantee

### Requirement: Token evolution preserves usable existing contracts

The public page envelope SHALL retain `items` and optional opaque-string `next` fields, and new tokens SHALL have an identifiable version. The History and Timeline contract cutover SHALL replace flat per-match items with group segments and reject every old per-match/phase continuation with HTTP 400 `INVALID_CURSOR`, including a complete old key. A fresh request SHALL return only the replacement grouped shape. These affected endpoints SHALL NOT keep an old mode, adapter, or reader solely for older clients, and deployment/rollback SHALL NOT require preserving their pre-cutover cursors. Updated clients SHALL use the replacement contract directly; no old-client capability negotiation or app-upgrade-free transition is promised.

For unaffected queries, complete valid legacy tokens SHALL remain accepted, incomplete tokens SHALL be rejected without guessing missing keys, and all consumers SHALL retain their existing compatible-reader rollout and rollback behavior. The independent ranked match-bets continuation contract and background layout/fan-out contracts SHALL remain outside the affected feed cutover. Dropping client compatibility SHALL NOT permit loss or duplicate processing of accepted backend work; active operations and queued/checkpointed work SHALL be drained or explicitly migrated before incompatible worker-state changes.

#### Scenario: A complete legacy base-table token is resumed
- **GIVEN** an old token contains the complete valid key for the unaffected layout-match query
- **WHEN** it is submitted after the grouped feed cutover
- **THEN** that unrelated query continues to resume at the same boundary

#### Scenario: An incomplete old index token is resumed after deployment
- **GIVEN** a caller submits an old finished/Timeline token, whether its old keys are complete or incomplete
- **WHEN** the replacement endpoint receives it
- **THEN** append is rejected with HTTP 400 `INVALID_CURSOR` and a first-page restart indication
- **AND** a fresh request returns the new grouped contract for an updated client, without an old response adapter

#### Scenario: Ranked match-bets token is already valid
- **GIVEN** a caller holds a valid ranked match-bets continuation
- **WHEN** it requests the same ranked context after this cutover
- **THEN** the unrelated ranked cursor remains usable with its existing ordering and context checks

#### Scenario: Backend versions overlap or a deployment is rolled back
- **GIVEN** deployment or rollback changes the affected feed/worker contracts
- **WHEN** traffic and background work switch releases
- **THEN** affected feeds use a coordinated backend/client contract and incompatible affected cursors are invalidated explicitly
- **AND** accepted queued or checkpointed work is preserved through draining or explicit migration without losing or duplicating awards
- **AND** unaffected endpoint readers retain their existing compatibility guarantees

### Requirement: Composed and background feeds retain continuation progress

Timeline SHALL use unsettled-before-finished phases at the kickoff-group level: a group with any uncomputed member remains in the unsettled phase, including its visible computed children, and enters the finished phase as one logical group when complete. The feed SHALL exhaust its current phase's continuation before moving to the next; empty storage pages SHALL NOT imply completion. The old per-match Timeline mode and phase wrappers SHALL be replaced. Background gamblers-by-layout fan-out and base-table layout-match traversal SHALL follow valid continuations through empty pages until completion. A pagination failure SHALL leave work failed or retryable under the existing orchestration rules, never acknowledged as a complete traversal. Materialization, scoring, membership, and replay safeguards SHALL remain in force.

#### Scenario: Timeline has an empty live page before more live entries
- **GIVEN** a grouped Timeline unsettled-phase storage page contains no returned entries but has a valid continuation
- **WHEN** Timeline returns the page and receives its next request
- **THEN** it remains in the unsettled phase and resumes that continuation
- **AND** it does not skip remaining groups by switching to the finished phase early

#### Scenario: Timeline reaches the live-to-finished boundary
- **GIVEN** unsettled groups are exhausted and finished groups span multiple storage pages
- **WHEN** Timeline continues
- **THEN** it transitions to finished groups using the replacement phase/revision-bound continuation
- **AND** completion of the finished phase exposes no further continuation

#### Scenario: Grouped Timeline contains computed and pending members together
- **GIVEN** a two-match group has one computed child and one eligible uncomputed child
- **WHEN** the grouped feed crosses storage pages or phases
- **THEN** both children belong to one logical group in the unsettled phase
- **AND** the computed child is not duplicated in the finished phase

#### Scenario: Fan-out spans multiple gambler pages
- **GIVEN** eligible gamblers for a layout span multiple index pages
- **WHEN** match fan-out consumes the query
- **THEN** it reaches every page using complete continuations
- **AND** it preserves existing guarded materialization and replay behavior

#### Scenario: Background continuation fails mid-traversal
- **GIVEN** fan-out has processed an earlier page and a later continuation fails
- **WHEN** that failure reaches the operation boundary
- **THEN** it is not treated as end of data or acknowledged as completed fan-out
- **AND** existing recovery can retry the unfinished work without bypassing materialization safeguards

## ADDED Requirements

### Requirement: Grouped feeds preserve authoritative group boundaries

The replacement grouped History and Timeline contracts SHALL use an `items` page and optional opaque-string `next`. Each group or group segment SHALL carry authoritative identity, complete group counts, computation state, a required explicit movement state, and published revision independently of returned member count. History SHALL include computed children only and order groups by kickoff descending. Timeline SHALL order unsettled groups by kickoff ascending followed by finished groups by kickoff descending. Both SHALL order children by match identifier ascending. A group too large for one bounded response SHALL continue as segments of the same logical group before the feed advances to another group. A repeated request with the same valid continuation SHALL reproduce its boundary for the same revision. Fetching a page SHALL NOT require reading the full pool, unbounded refill, or reconstructing standings on demand. Visibility and authorization SHALL be enforced independently of any stored cursor or projection.

#### Scenario: A group crosses a page boundary
- **GIVEN** a group contains four matches but the first response contains two
- **WHEN** the next segment loads
- **THEN** both responses identify the same group and report its full authoritative count and movement
- **AND** the client appends the remaining children under one logical header without recalculating movement from loaded children

#### Scenario: A partial group appears in History
- **GIVEN** one of two matches is computed
- **WHEN** History requests the grouped feed
- **THEN** it receives the computed child with total count two and computed count one
- **AND** it neither receives an invented pending History row nor reports a complete one-match group

#### Scenario: A known group has no computed children
- **GIVEN** a group has no computed matches and at least one eligible visible Timeline child
- **WHEN** the grouped feeds are requested
- **THEN** Timeline returns the group in the unsettled phase with its eligible visible children, canonical identity, kickoff, publication revision, full count, computed count zero, and explicit pending movement without numeric rank boundaries or movement
- **AND** History omits the group because it has no computed children
- **AND** a page segment's returned child count does not replace the group's authoritative full or computed counts

#### Scenario: Groups share a calendar date
- **GIVEN** two distinct kickoff instants occur on the same day
- **WHEN** their grouped feed pages are traversed
- **THEN** they remain separate logical groups in the requested phase and kickoff order
- **AND** member ties use match identifier ascending

### Requirement: Grouped continuations bind context and publication revision

Grouped continuations SHALL bind the authenticated read scope, pool, selected gambler, endpoint, phase, ordering version, published revision, and complete storage/member boundary. Invalid, cross-context, old-format, or superseded-publication tokens SHALL fail explicitly with HTTP 400 `INVALID_CURSOR` and first-page restart guidance; they SHALL NOT silently restart an append. A request across a publication change SHALL require that explicit restart instead of continuing a retained revision. Clients SHALL discard old append state on restart, replace the feed from its first page, and prevent superseded responses from merging. Refresh SHALL select the newest available coherent publication; it SHALL NOT wait indefinitely for a replay. Tokens for unrelated endpoints, including ranked match bets, SHALL not be interpreted as grouped-feed tokens or changed by this cutover.

#### Scenario: A correction changes history during pagination
- **GIVEN** the first page belongs to revision R and an older correction publishes revision R+1
- **WHEN** the next page is requested with R's continuation
- **THEN** it returns an explicit invalid-cursor first-page restart indication
- **AND** it never silently returns R+1 children or movement mixed into R's groups

#### Scenario: Group changes phase while pages are loading
- **GIVEN** a partial group is in Timeline's unsettled phase in revision R
- **WHEN** its last member computes and revision R+1 moves it to finished
- **THEN** R's continuation receives an explicit invalid-cursor restart response
- **AND** a fresh traversal contains the group once in its new phase

#### Scenario: A continuation is reused for another gambler
- **GIVEN** a grouped token belongs to gambler A in a pool
- **WHEN** it is submitted for gambler B or another endpoint
- **THEN** the service rejects it before serving data under the wrong context

#### Scenario: A stale page completes after refresh
- **GIVEN** a client refreshed into a newer publication
- **WHEN** an earlier append response arrives late
- **THEN** the earlier response does not replace or append to the refreshed group state
