# backend-pagination Specification

## Purpose

Let Fortuna clients and background consumers traverse backend query results using complete continuations, with reliable completion and explicit recovery from invalid tokens.

## Requirements

### Requirement: Continuations preserve the complete storage boundary

Every backend operation that exposes a serialized DynamoDB continuation SHALL preserve every attribute name, scalar type, and value required to resume the underlying table or index query. String and binary values SHALL round-trip without alteration; numeric values SHALL retain their DynamoDB number type and precision. The next storage request SHALL use the complete boundary returned by the preceding storage page, including when the last evaluated item was filtered out. Continuations SHALL NOT be reconstructed from only the last returned item, base-table keys, or an application offset.

This applies to pending bets, live/unsettled bets, finished bets, legacy match bets, a gambler's pool scores, a pool's leaderboard, pool members by username, gamblers by pool layout for fan-out, and open pool layouts. The shared base-table layout-match continuation SHALL remain correct as well.

#### Scenario: Leaderboard continuation contains a numeric score
- **GIVEN** a leaderboard storage page ends with string table keys and a numeric score key
- **WHEN** its returned token is supplied to the next-page request
- **THEN** the resumed query includes all of those keys with the same values and types
- **AND** the score is not omitted or converted into a string attribute

#### Scenario: Other index key shapes cross a page boundary
- **GIVEN** a page from any affected operation ends with a date, username, status, or composite index key in addition to table keys
- **WHEN** its continuation is encoded, transported, and decoded
- **THEN** every required index and table key survives unchanged
- **AND** the next page can be queried without an incomplete-start-key failure

#### Scenario: Scalar values require lossless representation
- **GIVEN** a continuation contains Unicode string content, a zero or negative number, a precision-sensitive number, or binary key bytes
- **WHEN** the continuation makes a serialization round trip
- **THEN** each attribute retains its original DynamoDB scalar type and exact value

### Requirement: Page completion follows the continuation key

Backend pages SHALL expose no `next` token when DynamoDB returns an absent or empty continuation key. A nonempty continuation key SHALL produce a usable token even when the page contains no matching items. Neither empty results nor an application-level batch size SHALL imply storage traversal is complete. Repository pagination SHALL NOT introduce unbounded refill loops to conceal empty pages.

#### Scenario: Final key is absent or an empty map
- **GIVEN** the final storage response has either no continuation key or an empty key map
- **WHEN** the backend constructs its page response
- **THEN** the optional `next` is absent or null according to the existing response encoding
- **AND** it does not attempt to serialize missing attributes or emit an empty-string token

#### Scenario: Filtering removes every item on a nonfinal page
- **GIVEN** a query evaluates a page whose items are all filtered out and returns a nonempty continuation
- **WHEN** that page is returned or consumed internally
- **THEN** its continuation remains available
- **AND** traversal can reach later matching items without restarting at page one

### Requirement: Traversal preserves query behavior and retry boundaries

For unchanged data, a fixed query scope, and converged index state, following newly issued continuations to completion SHALL visit every matching item once in the query's existing order. Repeating a request with the same continuation SHALL reproduce its boundary rather than advance it through hidden server state. The fix SHALL preserve existing filters, ascending or descending order, and tie behavior; it SHALL NOT introduce a new ordering guarantee for existing tied index values. Continuations SHALL NOT claim a frozen snapshot when records or index state change.

#### Scenario: Equal index values span several pages
- **GIVEN** unchanged matching records share a score, timestamp, or username across at least three storage pages
- **WHEN** all returned continuations are followed
- **THEN** no matching record is skipped or duplicated because the cursor lost its table or index keys
- **AND** the underlying query's existing order and tie behavior are retained

#### Scenario: A next-page request is retried
- **GIVEN** a page request failed transiently and its query scope and underlying data remain unchanged
- **WHEN** the caller retries with the same token
- **THEN** it resumes the same storage boundary and returns the same page contents

#### Scenario: Data changes between requests
- **GIVEN** scores, names, dates, membership, or index visibility change during traversal
- **WHEN** the caller refreshes without a continuation
- **THEN** a new traversal begins under the existing query and consistency rules
- **AND** no snapshot guarantee is inferred from the presence of a token

### Requirement: Invalid continuations fail explicitly

Before issuing a query with a supplied continuation, the backend SHALL validate its supported encoding, version, scalar values, required key names and types, and compatibility with partition or prefix constraints of the requested query. Malformed, incomplete, unsupported, or incompatible continuations SHALL fail explicitly. Both Lambda and HttpApi HTTP paths SHALL return HTTP 400 with error code `INVALID_CURSOR` and an indication to restart from the first page. Internal consumers SHALL surface a continuation failure through existing failure handling rather than report successful completion. Unrelated service outages SHALL NOT be mislabeled as invalid tokens.

The backend SHALL NOT silently restart an append, manufacture missing key values, or use token contents in place of the requested scope or existing access checks.

#### Scenario: A required index key is missing or has the wrong type
- **GIVEN** a supplied token omits an index key or represents a numeric score as a string attribute
- **WHEN** the corresponding endpoint handles the continuation
- **THEN** it returns HTTP 400 with `INVALID_CURSOR` and a first-page restart indication
- **AND** it issues no query using the invalid key

#### Scenario: Unsupported or malformed token
- **GIVEN** a token has an unsupported version, invalid scalar representation, unsupported key type, or malformed encoding
- **WHEN** either HTTP host receives it
- **THEN** the request produces the same controlled invalid-cursor response
- **AND** no raw parse exception or misleading empty-success page is returned

#### Scenario: Token belongs to a different query partition
- **GIVEN** a token's keys conflict with the requested pool, gambler, match, layout, or index key shape
- **WHEN** the continuation is validated
- **THEN** the request is rejected before querying with those keys
- **AND** token contents do not replace authorization or prediction-visibility enforcement

#### Scenario: Storage is unavailable for a valid continuation
- **GIVEN** a token is valid for its query but the database request fails for an unrelated reason
- **WHEN** the failure is handled
- **THEN** it follows existing service-failure behavior and is not reported as `INVALID_CURSOR`

### Requirement: Token evolution preserves usable existing contracts

The public page contract SHALL retain its `items` and optional opaque-string `next` fields. New tokens SHALL have an identifiable version. Existing clients SHALL be able to return those strings without understanding index keys. A legacy token SHALL remain accepted when it contains the complete valid key for its requested query, including the existing base-table layout-match query. An incomplete legacy index token SHALL be rejected explicitly; the backend SHALL NOT guess its missing historical key values from current records. Refreshing without a token SHALL yield a fresh page and a usable new continuation when needed.

Every relevant API and worker consumer SHALL be able to read new-format tokens whenever such tokens can reach it. When clients may hold tokens across a deployment, compatible readers SHALL be deployed before new-format emission is enabled; a release with no live traffic MAY ship reader and writer together. Rollback SHALL retain the ability to read tokens already issued by the new writer. The ranked match-bets continuation contract SHALL remain independently supported.

#### Scenario: A complete legacy base-table token is resumed
- **GIVEN** an old token contains both string table keys and no other key is required by the layout-match query
- **WHEN** it is submitted after the codec upgrade
- **THEN** the query resumes at that boundary successfully

#### Scenario: An incomplete old index token is resumed after deployment
- **GIVEN** a client holds a token issued by the old backend that discarded an index key
- **WHEN** the client submits it and then refreshes without that token
- **THEN** the append is rejected with the controlled invalid-cursor response
- **AND** the refresh starts a usable traversal without a mobile app upgrade

#### Scenario: Ranked match-bets token is already valid
- **GIVEN** a client holds a valid ranked match-bets continuation from before this repair
- **WHEN** it requests the same ranked context after deployment
- **THEN** the existing ranked cursor remains usable with its ordering and context checks intact

#### Scenario: Backend versions overlap or a deployment is rolled back
- **GIVEN** a new-format token has been issued
- **WHEN** another compatible deployment instance or the rollback release handles the next request
- **THEN** that reader can decode and validate the token
- **AND** rollout does not route new tokens to a reader that supports only the original two-field format

### Requirement: Composed and background feeds retain continuation progress

Timeline SHALL retain its live/unsettled-then-finished phase behavior and opaque phase continuations, changing phase only after the current phase's storage continuation is exhausted. Background gamblers-by-layout fan-out and base-table layout-match traversal SHALL follow valid continuations through empty pages until completion. A pagination failure SHALL leave work failed or retryable under the existing orchestration rules, never acknowledged as a complete traversal. This repair SHALL preserve existing materialization, scoring, membership, and replay safeguards.

#### Scenario: Timeline has an empty live page before more live entries
- **GIVEN** a live-phase page contains no returned entries but has a valid continuation
- **WHEN** Timeline returns the page and receives its next request
- **THEN** it remains in the live phase and resumes that continuation
- **AND** it does not skip remaining live entries by switching to finished entries early

#### Scenario: Timeline reaches the live-to-finished boundary
- **GIVEN** live entries are exhausted and finished entries span multiple storage pages
- **WHEN** Timeline continues
- **THEN** it transitions to finished entries and carries their complete continuation inside the existing phase wrapper
- **AND** completion of the finished phase exposes no further continuation

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
