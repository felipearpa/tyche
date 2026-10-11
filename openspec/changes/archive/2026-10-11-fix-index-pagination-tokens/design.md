# Design

## Context

See [proposal.md](proposal.md) for motivation. Persistence ownership and index schemas are documented in [the database model](../../../docs/database-model.md). The backend uses `IKeySerializer` in four repository implementations; the current `DynamoDbKeySerializer` serializes an anonymous record containing only the string values of `pk` and `sk`. Deserialization constructs only those two attributes. None of the affected request builders reconstructs the discarded index keys.

### Confirmed failure

On 2026-10-10, two read-only AWS CLI `Query` calls targeted `Pool/GetPoolGamblerScoresByPool-index` in `us-east-2`, with `pk = POOL#pagination-spec-probe-01a1287e`, `Limit = 1`, and automatic pagination disabled. No application records were written or returned.

| Exclusive start key | Result |
|---|---|
| `pk` (S) and `sk` (S), matching the existing serializer's output | `ValidationException: The provided starting key is invalid` |
| Identical `pk` and `sk`, plus `score` (N) = `120` | Success; `Count = 0`, `ScannedCount = 0`, no continuation |

The partition and member identifiers were synthetic. The only difference between the requests was the missing numeric index key. This confirms service rejection of the lossy key shape, not a production user incident or a full multi-page traversal. Source inspection confirms that the affected repositories actually produce that shape. The existing serializer tests only verify calls to a mocked JSON serializer; they do not verify retained fields or round trips.

[AWS pagination documentation](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Query.Pagination.html) requires using the returned `LastEvaluatedKey` as the next request's `ExclusiveStartKey`. [The Query API](https://docs.aws.amazon.com/amazondynamodb/latest/APIReference/API_Query.html) permits string, number, and binary key values. A nonempty continuation can accompany an empty filtered page; an absent or empty key ends traversal.

### Affected query inventory

All rows below currently pass through the shared serializer. Key attributes listed are in addition to the base table's string `pk` and `sk`.

| Repository operation | Index | Discarded key attributes | Consumer |
|---|---|---|---|
| `PoolGamblerBetDynamoDbRepository.GetPendingPoolGamblerBetsAsync` | `GetPendingPoolGamblerBets-index` | `matchDateTime` (S) | Pending bets |
| `PoolGamblerBetDynamoDbRepository.GetLivePoolGamblerBetsAsync` | `GetPendingPoolGamblerBets-index` | `matchDateTime` (S) | Live/unsettled bets and Timeline's first phase |
| `PoolGamblerBetDynamoDbRepository.GetFinishedPoolGamblerBetsAsync` | `GetFinishedPoolGamblerBets-index` | `computedDateTime` (S) | History and Timeline's second phase |
| `PoolGamblerBetDynamoDbRepository.GetPoolMatchGamblerBetsAsync` | `GetPoolGamblerScoresByMatch-index` | `getPoolGamblerScoresByMatchPk`, `getPoolGamblerScoresByMatchSk` (S) | Legacy match-bets endpoint |
| `PoolGamblerScoreDynamoDbRepository.GetGamblerScoresAsync` | `GetPoolGamblerScoresByGambler-index` | `score` (N) | My pools |
| `PoolGamblerScoreDynamoDbRepository.GetPoolScoresAsync` | `GetPoolGamblerScoresByPool-index` | `score` (N) | Leaderboard |
| `PoolDynamoDbRepository.GetPoolMembersAsync` | `GetPoolGamblersByUsername-index` | `gamblerUsername` (S) | Member management |
| `PoolDynamoDbRepository.GetGamblersByPoolLayoutAsync` | `GetGamblersByPoolLayout-index` | `getGamblersByPoolLayoutPk`, `getGamblersByPoolLayoutSk` (S) | `FanOutMatchToGamblers` |
| `PoolLayoutDynamoDbRepository.GetOpenPoolLayoutsAsync` | `GetOpenedPoolLayout-index` | `status`, `startDateTime` (S) | Open layouts |

There are nine affected operations over eight distinct indexes. The tenth shared-codec caller, `GetPoolLayoutMatchesThroughVersionAsync`, queries the base table and needs only `pk` and `sk`; preserve its existing continuations and include it in end-of-list coverage.

Paths inspected but not affected by this field-loss defect:

- Ranked match bets use `RankedMatchBetsCursor`, which retains or reconstructs all four required key attributes and validates ranked context.
- Username propagation (`GetGamblerScoreItemsRequestBuilder`) passes raw keys between queries. Settlement's match-index discovery also passes raw keys. They share indexes with affected reads but do not use the lossy serializer.
- Base-table settlement/projection cursors and deletion loops use their own key continuation paths.
- Account email lookup has no paginated `next` contract.

## Goals / Non-Goals

**Goals:** Provide one lossless scalar-key codec, consistent completion handling in every shared-codec caller, and explicit invalid-token behavior without additional database reads per page. Keep repositories responsible for data access, with Timeline and fan-out traversal in the application layer.

**Non-Goals:** Changing table/index definitions, adding a pagination service or stored cursors, changing page sizes/order/tie rules, inventing snapshot consistency, migrating the ranked cursor, changing mobile presentation, or repairing the separate iOS optional-parameter `next=nil` issue. Ordinary client paging batches are not necessarily DynamoDB storage pages.

## Decisions

### 1. Use a versioned typed key envelope

Replace the two-field projection with a shared envelope containing a version and the entire key map. The initial format is JSON with `v: 1` and `key`, whose entries use exactly one of `S`, `N`, or `B`. For example:

```json
{"v":1,"key":{"pk":{"S":"POOL#example"},"sk":{"S":"GAMBLER#example"},"score":{"N":"120"}}}
```

Keep the result an opaque string in the existing `next` field; existing HTTP clients already encode string query parameters. Preserve attribute names and string values exactly, number values as their DynamoDB decimal strings without floating-point conversion, and binary values as base64-encoded bytes. Do not serialize the AWS SDK object graph or accept document/set/boolean/null attribute types as keys. Use immutable intermediate values; construct SDK dictionaries only at the boundary.

The core codec is independent of Pool/PoolLayout attribute names. Avoid per-index serializers: another hardcoded projection would recreate the same defect for a future index. Avoid raw SDK JSON because its unset fields and object representation are not the public token contract.

### 2. Validate a decoded key against the requested query

The codec validates envelope/version/type structure. Request construction validates the exact union of table and index key names, the expected types, and partition/prefix constraints implied by its query. These small static contracts belong with the request builders; do not call `DescribeTable` per page. A shared invalid-continuation failure distinguishes decode/shape/scope errors from service outages.

Both HTTP hosts map that failure to HTTP 400 with error code `INVALID_CURSOR` and a first-page restart indication. Do not translate unrelated DynamoDB failures to cursor errors. Internal consumers propagate the failure through their existing failure/retry handling instead of interpreting it as successful completion. Cursor contents never replace the requested query scope, authorization, or prediction-visibility checks. Signing/encryption and binding every filter or clock value are outside this repair.

### 3. Keep legacy compatibility honest

Decode the previous unversioned `{ "pk": "...", "sk": "..." }` shape as two string attributes, then apply the same query-key validation. It remains valid for the base-table layout-match query. For every affected index it is incomplete and must fail explicitly before the database query. The old serializer already discarded the missing values, so it cannot restore the original boundary.

Do not read the current item to guess an old score, name, or date: it might have changed or been removed. Do not silently replace an append with page one, which could duplicate rows. Existing clients can recover through an ordinary refresh or reopening the list; no new automatic restart is promised. Legacy match-bets requests remain available in legacy order, while invalid legacy continuation keys get a controlled error rather than the existing service failure. Preserve the independently supported ranked cursor exactly.

### 4. Normalize completion at the shared boundary

Provide one optional-key-to-token helper used by all ten shared-codec operations: absent key or zero entries becomes `None`; a nonempty key is encoded in full. Do not decide completion from `Items`, `Count`, or the application page size. The current `Option.ofObj` checks alone do not protect against a non-null empty dictionary and can attempt to serialize nonexistent `pk`/`sk` entries.

Return empty filtered pages with their continuation instead of adding an unbounded refill loop in repositories. Existing fan-out/materialization loops must continue according to `Next`. Timeline must preserve `live:` and `finished:` wrappers, continue empty live pages when a key exists, and switch to finished entries only when live continuation is exhausted. Keep its current unprefixed input interpretation for otherwise valid inner tokens.

### 5. Verify actual boundaries, not only mocks or screen scrolling

Use real JSON round-trip assertions for string, numeric (including zero, negative and precision-sensitive values), binary, Unicode, and compound-key cases. Reject malformed/unknown-version/unsupported-type tokens and query-specific missing/wrong keys. Assert empty-key normalization for every shared-codec caller.

For each of the nine index operations, force small query limits in the test client adapter or fixture rather than changing production page sizes. Capture the first response's full key, feed the emitted token through the same repository's next call, and assert exact key names/types/values on the resulting request. Traverse at least three storage pages with unchanged fixture data; include ties, ascending/descending paths, empty filtered pages, final empty keys, and retrying the same token. Existing fake DynamoDB support can assist, but cannot substitute for service validation.

Run an isolated DynamoDB integration fixture with the eight index schemas and a base-table query, using explicit convergence polling with a deadline before traversal. Require successful continuations for every affected operation and the old lossy key's failure. Reuse an existing authorized test environment where available; keep test data out of production application partitions. Verify Timeline phase transitions, background fan-out completion, both HTTP hosts' invalid-token responses, and ranked/raw-key paths as regression controls. Follow repository ARTEMIS prerequisites before authoring tests and record mobile transport smoke checks separately from storage-pagination proof.

## Risks / Trade-offs

- **Old in-flight index tokens cannot be repaired** → Explicit 400 and ordinary refresh; retain old base-table tokens that are complete.
- **New tokens reach old backend readers during deployment or rollback** → Acceptable for this release because there is no production traffic; never roll back below the dual-format reader. Future token-format changes with live traffic deploy readers before enabling new emission.
- **Keys move while paging** → Promise complete traversal only for unchanged data/query scope and converged indexes; preserve existing refresh behavior and eventual consistency.
- **A fake client overlooks DynamoDB key validation** → Include service-backed key validation and all-schema traversal, in addition to repository request assertions.
- **A normal test dataset never crosses a storage boundary** → Force small limits in fixtures and record evaluated-page counts; client-side batches do not establish coverage.

## Migration Plan

1. Land codec/query validation, controlled error mapping, completion handling, the typed writer, and regression coverage as one release. On 2026-10-11 Fortuna has no open pools and no production traffic, so no client holds a token across the deployment and a separate reader-only revision is unnecessary. Document the compatibility behavior.
2. Deploy that release to all Lambda/API and worker consumers. Existing working base-table continuations still work; incomplete legacy index continuations receive controlled errors.
3. Verify all affected endpoint and worker paths, existing mobile token transport, and no new invalid-key errors for newly issued tokens. Replay previously failed fan-out through the existing recovery procedure where needed; do not acknowledge work merely because it failed at a page boundary.
4. If rollback is necessary, retain the dual-format reader and error mapping. Never roll back to the original serializer after new tokens have been issued: the original reader fails on them with a server error (confirmed in task 3.3). No item backfill, table migration, or index deployment is needed.
5. Once there is live traffic, any future token-format change ships readers first and the writer in a later revision, because old and new instances overlap during a deployment and a rollback would route new tokens to old readers. A separate long-lived feature flag is not required.
