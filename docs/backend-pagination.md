# Backend pagination

Paginated backend reads return a page of `items` and an optional `next` string. `next` is an opaque continuation token: clients send it back unchanged to get the following page and never parse it. This document describes the token format that `Felipearpa.Data.DynamoDb` produces and accepts for DynamoDB queries, and how a page decides it is the last one.

The ranked match-bets feed uses its own cursor (`RankedMatchBetsCursor`) and is not covered here. Table and index key schemas are in [the database model](database-model.md).

## Token format, version 1

A version 1 token is a JSON object with exactly two members:

- `v`: the integer `1`.
- `key`: DynamoDB's `LastEvaluatedKey`, one member per key attribute. Each attribute is an object with exactly one of:
  - `S`: the string value, unchanged.
  - `N`: the number as DynamoDB's decimal text, unchanged (`"120.50"` stays `"120.50"`; it is never converted to a floating-point or decimal value).
  - `B`: the binary value as standard base64.

The writer emits attributes in ordinal order of their names and escapes non-ASCII characters and HTML-sensitive characters such as `+` and `&` as `\uXXXX`; other characters, including `/`, `?`, `#`, `%`, and `=`, are written as is, so clients must URL-encode the token as they do any query parameter. Readers do not depend on member order or escaping. For example, a leaderboard page that ends on a membership with score 120 produces:

```json
{"v":1,"key":{"pk":{"S":"POOL#example"},"score":{"N":"120"},"sk":{"S":"GAMBLER#example"}}}
```

and both of these decode to the same four attributes (binary `AAH/` is the bytes `00 01 FF`):

```json
{"v":1,"key":{"pk":{"S":"POOL#example"},"sk":{"S":"GAMBLER#example"},"score":{"N":"120"},"blob":{"B":"AAH/"}}}
{"key":{"blob":{"B":"AAH/"},"score":{"N":"120"},"sk":{"S":"GAMBLER#example"},"pk":{"S":"POOL#example"}},"v":1}
```

The codec is independent of table and attribute names. It carries whatever key DynamoDB returned, including index keys such as `score`, `matchDateTime`, `gamblerUsername`, or composite index attributes.

## Legacy format

Before version 1 the backend wrote only the string table keys:

```json
{"pk":"POOLLAYOUT#01L","sk":"MATCH#01M"}
```

A token without `v` is read as this legacy shape: exactly the members `pk` and `sk`, both nonempty JSON strings, decoded as two `S` attributes. That is a complete key for base-table queries such as layout matches. For an index query it is missing the index keys, and the query's key validation must reject it; the missing values are never looked up or guessed.

## Decoding rules

`ContinuationToken.decode` checks the token's envelope, version, scalar types, and values. It does not know which query the token is for; each query then checks the decoded key against its own start-key contract (see [Query start keys](#query-start-keys)).

| Token | Result |
|---|---|
| Not JSON, not an object, empty, or blank | `MalformedToken` |
| `v` present but not an integer, `key` missing or not an object, extra top-level members, duplicate members | `MalformedToken` |
| `v` is an integer other than `1` | `UnsupportedVersion` |
| `key` has no attributes, or an attribute is not an object with exactly one type member | `MalformedToken` |
| An attribute's type is anything but `S`, `N`, or `B` (for example `SS`, `M`, `BOOL`, `NULL`) | `UnsupportedAttributeType` |
| A value is not a JSON string, an `S` or `B` value is empty, `B` is not base64, `S` is not valid text, or `N` is not DynamoDB number syntax (optional `-`, digits, optional fraction, optional exponent) or has more than 38 significant digits | `InvalidAttributeValue` |
| No `v` and anything other than exactly string `pk` and `sk` | `MalformedToken` or `InvalidAttributeValue` |

All cases are values of `InvalidContinuation`. `IKeySerializer.Deserialize` raises them as `InvalidContinuationException`. Every case means the same thing to a client: the token cannot be used and the list must be loaded again from the first page. A DynamoDB failure for a token that decoded successfully is a service failure, not an invalid token.

Number magnitude limits are left to DynamoDB.

## Page completion

`KeySerializer.next` turns a query response's `LastEvaluatedKey` into the page's `next`:

- An absent (`null`) key or an empty key map means the traversal is complete: `next` is absent.
- Any other key is encoded in full, even when the page has no items because a filter removed them all. The caller follows that token to reach later matches.

Completion never depends on the number of items or on the application's page size. `IKeySerializer.Serialize` rejects an empty key instead of producing an empty token.

## Query start keys

Each request builder declares a `StartKeyContract`: the exact table and index key attributes of its query's start key, each with its scalar type and, where the query fixes it, its partition value or key prefix. `StartKeyContract.startKey` decodes the token with `IKeySerializer.Deserialize` and checks it against the contract before the request is built, so an unusable token never reaches DynamoDB. The contracts are static; no table description is read.

A key that does not fit raises `InvalidContinuationException` with one of these `InvalidContinuation` cases, in addition to the decoding cases above:

| Case | Example |
|---|---|
| `MissingKeyAttribute` | A legacy `pk`/`sk` token sent to an index query, or a leaderboard token without `score` |
| `UnexpectedKeyAttribute` | A token with an attribute the query's key does not use |
| `WrongKeyAttributeType` | `score` sent as a string instead of a number |
| `KeyOutOfScope` | A token from another pool, gambler, match, or layout, or an open-layouts token whose `status` is not `OPENED` |

A client handles these exactly like a token that cannot be decoded: load the list again from the first page. The token's contents never replace the requested pool, gambler, match, or layout, or any access check.

`DynamoDbKeySerializer` writes version 1 tokens. A legacy token that a client still holds from the earlier backend is rejected by an index query with `MissingKeyAttribute`, where DynamoDB used to reject it with a validation error; a legacy base-table layout-match token keeps working.

### Coverage inventory

Every shared-codec query, its start key, and its checks. All keys also carry the table's string `pk` and `sk`.

| Operation | Request builder | Index | Index key attributes | Scope checked | Legacy `pk`/`sk` token |
|---|---|---|---|---|---|
| Pending bets (`GetPendingPoolGamblerBetsAsync`) | `GetPendingPoolGamblerBetsRequestBuilder` | `GetPendingPoolGamblerBets-index` | `matchDateTime` (S) | `pk` = `GAMBLER#<gamblerId>#POOL#<poolId>`, `sk` starts with `MATCH#` | Rejected |
| Live bets (`GetLivePoolGamblerBetsAsync`) | `GetLivePoolGamblerBetsRequestBuilder` (pending-bets contract) | `GetPendingPoolGamblerBets-index` | `matchDateTime` (S) | As pending bets | Rejected |
| Grouped History and Timeline's settled phase (`GetFinishedBetsByKickoffAsync`) | `GroupedBetFeedRequestBuilder.finishedPage` | `GetFinishedPoolGamblerBetsByMatchDate-index` | `computedMatchDateTime` (S) | As pending bets; a `computedDateTime` token is rejected with `MissingKeyAttribute` | Rejected |
| Timeline's unsettled phase (`GetUnsettledBetsByKickoffAsync`) | `GroupedBetFeedRequestBuilder.unsettledPage` (pending-bets contract) | `GetPendingPoolGamblerBets-index` | `matchDateTime` (S) | As pending bets | Rejected |
| Legacy match bets (`GetPoolMatchGamblerBetsAsync`) | `GetPoolMatchGamblerBetsRequestBuilder` | `GetPoolGamblerScoresByMatch-index` | `getPoolGamblerScoresByMatchPk`, `getPoolGamblerScoresByMatchSk` (S) | `getPoolGamblerScoresByMatchPk` and `sk` = `MATCH#<matchId>`, `getPoolGamblerScoresByMatchSk` starts with `POOL#<poolId>#`, `pk` starts with `GAMBLER#` | Rejected |
| A gambler's pool scores (`GetGamblerScoresAsync`) | `GetGamblerScoresRequestBuilder` | `GetPoolGamblerScoresByGambler-index` | `score` (N) | `sk` = `GAMBLER#<gamblerId>`, `pk` starts with `POOL#` | Rejected |
| Pool leaderboard (`GetPoolScoresAsync`) | `GetPoolScoresRequestBuilder` | `GetPoolGamblerScoresByPool-index` | `score` (N) | `pk` = `POOL#<poolId>`, `sk` starts with `GAMBLER#` | Rejected |
| Pool members (`GetPoolMembersAsync`) | `GetPoolMembersRequestBuilder` | `GetPoolGamblersByUsername-index` | `gamblerUsername` (S) | `pk` = `POOL#<poolId>`, `sk` starts with `GAMBLER#` | Rejected |
| Gamblers by layout, for fan-out (`GetGamblersByPoolLayoutAsync`) | `GetGamblersByPoolLayoutRequestBuilder` | `GetGamblersByPoolLayout-index` | `getGamblersByPoolLayoutPk`, `getGamblersByPoolLayoutSk` (S) | `getGamblersByPoolLayoutPk` = `POOLLAYOUT#<layoutId>`, `getGamblersByPoolLayoutSk` and `pk` start with `POOL#`, `sk` starts with `GAMBLER#` | Rejected |
| Open layouts (`GetOpenPoolLayoutsAsync`) | `GetOpenPoolLayoutRequestBuilder` | `GetOpenedPoolLayout-index` | `status`, `startDateTime` (S) | `status` = `OPENED`, `pk` and `sk` start with `POOLLAYOUT#` | Rejected |
| Layout matches (`GetPoolLayoutMatchesThroughVersionAsync`) | `GetPendingPoolLayoutMatchesRequestBuilder` | Base table | None | `pk` = `POOLLAYOUT#<layoutId>`, `sk` starts with `MATCH#` | Accepted: complete for this query |

Ranked match bets (`RankedMatchBetsCursor`), username propagation (`GetGamblerScoreItemsRequestBuilder`), settlement's match-index discovery, and the settlement and projection cursors use their own continuations and are not in this table.

Tests that cover the table, all run locally without AWS:

- `Felipearpa.Tyche.Pool/test/Infrastructure/SharedCodecPaginationTest.fs` runs each of the eleven operations against the in-memory DynamoDB fake with storage pages forced to two items. For every operation it checks the pages returned, the exact start key (names, S/N types, values) of every next-page request across at least three boundaries, an absent and an empty-map final key, and a failed then retried request resuming the same boundary. The fixtures include ties across boundaries (match dates, kickoffs, scores, usernames, start dates), score `0`, a non-ASCII username, and pages emptied by each query's filter (pending, live, and unsettled-by-kickoff computed bets, legacy match-bet lock time, closed memberships, layout version). It also checks that legacy tokens are rejected for the ten index queries and accepted for layout matches, and the invalid keys listed per operation.
- `Felipearpa.Tyche.Pool/test/Application/ContinuationTraversalTest.fs` covers fan-out and joined-gambler layout-match traversal across one-item pages, and failures on a later page that leave the work failed for redelivery.
- `Felipearpa.Tyche.Pool/test/Application/GroupedBetFeedTest.fs` covers the grouped History and Timeline: whole kickoff groups across storage pages and the continuation after a completed group, Timeline's `live:`/`finished:` wrappers at group level, empty unsettled pages that stay unsettled, the switch to settled groups only after unsettled ones end, unprefixed live tokens, and the rejection of `computedDateTime` tokens.
- `Felipearpa.Tyche.PoolLayout/test/Infrastructure/PoolLayoutPaginationTest.fs` uses scripted responses for an open-layouts page with no items and a key, legacy tokens for both layout queries, and another layout's token.
- `Felipearpa.Data.DynamoDb/test/StartKeyContractTest.fs` covers the contract check itself.
- `Felipearpa.Tyche.AmazonLambda/test/Function/InvalidCursorTest.fs` (Lambda handlers) and `Felipearpa.Tyche.HttpApi/test/InvalidCursorTest.fs` (HttpApi routes on a test server) send malformed, unsupported, legacy, wrongly typed, and other-scope tokens to all nine endpoints and expect the 400 `INVALID_CURSOR` body with no index query; a usable token reaches the query as its start key; a DynamoDB failure for a usable token is not turned into `INVALID_CURSOR`.
- `Felipearpa.Data.DynamoDb/test/RolloutCompatibilityTest.fs` covers the reader and writer combinations of the [rollout](#rollout): the released serializer against tokens from, and readers of, the original serializer.

The fake checks that a start key has exactly the table and index key attributes, as DynamoDB does, but not their types against a table schema.

### Service fixture

The verification harness has a `pagination` mode that runs the same operations against DynamoDB itself, on isolated copies of the `Pool` and `PoolLayout` tables (all eight index schemas and the base table):

```bash
cd Amazon.Lambda/operations
./pool-table.sh create-copy Pool-verify-<date> PoolLayout-verify-<date>
cd ../Felipearpa.Tyche.Verification
POOL_TABLE_NAME=Pool-verify-<date> POOL_LAYOUT_TABLE_NAME=PoolLayout-verify-<date> AWS_REGION=us-east-2 \
  dotnet run -- pagination pagination-results.json
aws dynamodb delete-table --region us-east-2 --table-name Pool-verify-<date>
aws dynamodb delete-table --region us-east-2 --table-name PoolLayout-verify-<date>
```

It refuses the production table names. It seeds a fixture whose ties, closed memberships, computed bets, and layout versions put page boundaries and filtered-empty pages where the checks need them, and reads through a client that gives each query without a `Limit` a limit of two items, so every operation crosses at least three storage pages; production requests are unchanged. For each operation it polls the whole traversal with a three-minute deadline until the indexes have converged, then checks that the version 1 tokens visit every expected item once, carry every key attribute with its type, fit the query's start-key contract, survive URL encoding in a query string unchanged, and follow index order at each boundary; that filtered-empty pages keep their token; that the last page has none; and that following the same tokens again gives the same pages. It also sends a legacy `pk`/`sk` token, as the earlier backend wrote it, which the query rejects before reaching DynamoDB, and the same `pk`/`sk` start key straight to DynamoDB, which rejects it as invalid (for layout matches both work). Timeline phases, ranked match bets, settlement's match-index discovery, and username propagation are checked as well. A second fixture checks the grouped History and Timeline through `GetFinishedPoolGamblerBets` and `GetGamblerBetsTimeline`: a storage page that ends inside a kickoff group returns the whole group once with its layout counts, a partial group appears once in Timeline's unsettled phase and, after its last result, once in the finished phase, and a token from the `computedDateTime` index is rejected before a first-page request succeeds. Run it on newly created tables: the open-layouts index spans every layout in the table, so a second run on the same tables finds the first run's layouts. Results go to the JSON file; the exit code is 1 when any check fails.

## Invalid cursor responses

Both HTTP hosts, the HttpApi routes and the Lambda functions, run every paginated endpoint through `ContinuationResponse.orInvalidCursor` in `Felipearpa.Tyche.Function`. When the `next` token raises `InvalidContinuationException`, for any decoding or start-key case above, the endpoint answers:

```http
HTTP/1.1 400 Bad Request
Content-Type: application/json; charset=utf-8

{"error":"INVALID_CURSOR","message":"The continuation is not valid; restart from the first page"}
```

The token is checked while the request is built, so no query runs with it. The endpoints are the leaderboard (`GET /pools/{poolId}/gamblers`), pending, live, and finished bets and the timeline (`GET /pools/{poolId}/gamblers/{gamblerId}/bets/pending|live|finished|timeline`), legacy match bets (`GET /pools/{poolId}/matches/{matchId}/bets` without `sort`), a gambler's pools (`GET /gamblers/{gamblerId}/pools`), pool members (`GET /pools/{poolId}/members`), and open layouts (`GET /pool-layouts/open`). The body has the same shape and code as the ranked feed's `INVALID_CURSOR` (see [pool settlement](pool-settlement.md)); the ranked feed keeps its own cursor and handling.

Recovery: the client discards the token and loads the list again without `next`, as a pull-to-refresh or reopening the screen does; for the timeline, that starts again with live entries. Sending the same token again fails the same way. The backend never answers an append with the first page, and it never fills in a missing key. The Fortuna apps need no change, since they already reload a list from its first page on refresh.

Only `InvalidContinuationException` is mapped. Every other failure keeps its existing handling: a DynamoDB error for a usable token, for example, is still an unhandled failure that the hosts answer with 500. Membership and ownership checks run as before; for pool members, a caller who does not own the pool gets 403 whatever the token.

Background consumers have no HTTP response: an invalid token, like any query failure, fails the fan-out or layout-match materialization, which is retried and never acknowledged as complete.

## Rollout

The version 1 reader and writer ship together in one release. On 2026-10-11 Fortuna had no production traffic and no open pools, so no client held a token across the deployment, and no running instance of the earlier backend would receive a version 1 token.

Deploy the release to every consumer of the shared serializer: the Lambda functions behind the API (`PoolFunction`, `GamblerFunction`, `PoolLayoutFunction`) and the stream and queue handlers that fan out and materialize matches, which `Amazon.Lambda/deploy-lambda.sh` deploys together as one stack, and the HttpApi host wherever it runs. No feature flag, table change, or data backfill is involved.

Tokens issued by the earlier backend:

- A legacy base-table token is complete and keeps working.
- A legacy index token never had the index keys; it gets `INVALID_CURSOR`, and the client recovers by refreshing (see [Invalid cursor responses](#invalid-cursor-responses)).

Rollback:

- Roll back only to a version that keeps the dual-format reader and the `INVALID_CURSOR` mapping.
- Never roll back to the original serializer once version 1 tokens exist: reading one fails with a raw `KeyNotFoundException` (a server error, not `INVALID_CURSOR`) because it looks only for top-level `pk` and `sk`.

`Felipearpa.Data.DynamoDb/test/RolloutCompatibilityTest.fs` checks each of these combinations.

### Future token-format changes

Once Fortuna has live traffic, old and new instances overlap during a deployment, and a rollback sends new tokens to old readers. Ship any later change to the token format in two revisions:

1. **Readers first.** Deploy a revision that reads both the current and the new format and still writes the current one, to every consumer listed above. Confirm that no instance of the earlier version is still serving.
2. **Writer later.** Deploy a revision that writes the new format. Roll back from it only to the reader revision, never further.

Keep compatibility tests for each reader and writer combination, as `RolloutCompatibilityTest.fs` does for this release.

### Replaying failed fan-out

A match added to a layout reaches every gambler through the `PoolLayout` stream. When fan-out fails on a later gambler page, the stream retries the batch, and after its retries the batch is kept in the failure bucket; nothing is acknowledged as complete. After the release is deployed, replay each retained match insertion with the existing procedure in [the operations runbook](../Amazon.Lambda/operations/README.md#retained-stream-failure): `./enqueue-maintenance.sh RECONCILE_POOL <poolId>` for each of the layout's pools, which materializes the missing details and requests settlement. Materialization is guarded per detail, so a replay never creates a bet twice. Delete the failure object once the replay is verified.
