# Spec Delta

## MODIFIED Requirements

### Requirement: Traversal preserves query behavior and retry boundaries

For unchanged data, a fixed query scope, and converged index state, following newly issued continuations to completion SHALL visit every matching item once in the query's existing order. Repeating a request with the same continuation SHALL reproduce its boundary rather than advance it through hidden server state. Unaffected queries SHALL preserve their existing filters, ascending or descending order, and tie behavior; the grouped History and Timeline feeds SHALL use their specified group order and child order. Continuations SHALL NOT claim a frozen snapshot when records or index state change.

#### Scenario: Equal index values span several pages
- **GIVEN** unchanged matching records share a score, timestamp, or username across at least three storage pages
- **WHEN** all returned continuations are followed
- **THEN** no matching record is skipped or duplicated because the cursor lost its table or index keys
- **AND** unaffected queries retain their existing order and tie behavior while grouped feeds use kickoff order and match-identifier order

#### Scenario: A next-page request is retried
- **GIVEN** a page request failed transiently and its query scope and underlying data remain unchanged
- **WHEN** the caller retries with the same token
- **THEN** it resumes the same storage boundary and returns the same page contents

#### Scenario: Data changes between requests
- **GIVEN** scores, names, dates, membership, or index visibility change during traversal
- **WHEN** the caller refreshes without a continuation
- **THEN** a new traversal begins under the existing query and consistency rules
- **AND** no snapshot guarantee is inferred from the presence of a token

### Requirement: Token evolution preserves usable existing contracts

The public page contract SHALL retain its `items` and optional opaque-string `next` fields. New tokens SHALL have an identifiable version. Existing clients SHALL be able to return those strings without understanding index keys. For unaffected queries, a legacy token SHALL remain accepted when it contains the complete valid key for its requested query, including the existing base-table layout-match query; an incomplete legacy index token SHALL be rejected explicitly without guessing missing key values. The grouped History feed and Timeline's finished phase SHALL read a different index than before; a continuation issued for the previous finished-bets index SHALL be rejected with HTTP 400 `INVALID_CURSOR` and a first-page restart indication, and a fresh request SHALL return the grouped contract. Those two feeds SHALL NOT keep an old response mode or adapter; backend and apps SHALL cut over together.

Every relevant API and worker consumer SHALL be able to read new-format tokens whenever such tokens can reach it. The ranked match-bets continuation contract SHALL remain independently supported and unchanged.

#### Scenario: A complete legacy base-table token is resumed
- **GIVEN** an old token contains both string table keys and no other key is required by the layout-match query
- **WHEN** it is submitted after the grouped-feed cutover
- **THEN** the query resumes at that boundary successfully

#### Scenario: An incomplete old index token is resumed after deployment
- **GIVEN** a client holds a History or Timeline finished-phase token issued before the cutover
- **WHEN** it submits the token and then refreshes without it
- **THEN** the append is rejected with the controlled invalid-cursor response
- **AND** the refresh returns the first grouped page with a usable new continuation

#### Scenario: Ranked match-bets token is already valid
- **GIVEN** a client holds a valid ranked match-bets continuation
- **WHEN** it requests the same ranked context after the cutover
- **THEN** the ranked cursor remains usable with its ordering and context checks intact

#### Scenario: Backend versions overlap or a deployment is rolled back
- **GIVEN** the matched backend and app release is rolled back
- **WHEN** the previous release handles the next request
- **THEN** unaffected endpoints keep their existing compatibility guarantees
- **AND** grouped-feed tokens are invalidated explicitly rather than decoded by the previous release

### Requirement: Composed and background feeds retain continuation progress

Timeline SHALL use unsettled-before-finished phases at the kickoff-group level: a group with any layout match that lacks a result belongs to the unsettled phase, including that gambler's already computed children, and enters the finished phase as one group once every match has a result. The feed SHALL exhaust the current phase's storage continuation before changing phase; empty storage pages SHALL NOT imply completion. The phase wrapper and its inner continuations SHALL keep their existing encoding. Background gamblers-by-layout fan-out and base-table layout-match traversal SHALL follow valid continuations through empty pages until completion. A pagination failure SHALL leave work failed or retryable under the existing orchestration rules, never acknowledged as a complete traversal. Materialization, scoring, membership, and replay safeguards SHALL remain in force.

#### Scenario: Timeline has an empty live page before more live entries
- **GIVEN** an unsettled-phase storage page contains no returned entries but has a valid continuation
- **WHEN** Timeline returns the page and receives its next request
- **THEN** it remains in the unsettled phase and resumes that continuation
- **AND** it does not skip remaining groups by switching to the finished phase early

#### Scenario: Timeline reaches the live-to-finished boundary
- **GIVEN** unsettled groups are exhausted and finished groups span multiple storage pages
- **WHEN** Timeline continues
- **THEN** it transitions to finished groups inside the existing phase wrapper
- **AND** completion of the finished phase exposes no further continuation

#### Scenario: A partial group is not shown twice
- **GIVEN** a two-match group where one match has a result and the other does not
- **WHEN** Timeline is traversed through both phases
- **THEN** the group appears once, in the unsettled phase, with both children
- **AND** the finished phase skips it

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

### Requirement: Grouped feeds return whole kickoff groups

The grouped History and Timeline feeds SHALL return `items` as kickoff groups and an optional opaque `next`. A group SHALL NOT be split across pages: when a storage page ends inside a group, the service SHALL complete that group before returning the page and SHALL issue a continuation that resumes after it. History SHALL order groups by kickoff descending and include computed children only. Timeline SHALL order unsettled groups by kickoff ascending and finished groups by kickoff descending. Children SHALL be ordered by match identifier ascending. Fetching a page SHALL require at most the storage page, the completion query for the boundary group, the layout read for counts, and, in Timeline's unsettled phase, one query per group for all of that gambler's children at the group's kickoff. Visibility and authorization SHALL be enforced on every request independently of any token.

#### Scenario: A page boundary falls inside a group
- **GIVEN** a four-match group whose first two children end a storage page
- **WHEN** that page is returned
- **THEN** the page contains all four children under one group with its full counts and movement
- **AND** the continuation resumes with the next group

#### Scenario: Two groups share a calendar date
- **GIVEN** two distinct kickoff instants on the same day
- **WHEN** a grouped page is traversed
- **THEN** they are separate groups in the requested order
- **AND** children within each are ordered by match identifier

#### Scenario: A continuation is reused for another gambler
- **GIVEN** a grouped token issued for gambler A in a pool
- **WHEN** it is submitted for gambler B or another pool
- **THEN** the service rejects it before serving data under the wrong context
