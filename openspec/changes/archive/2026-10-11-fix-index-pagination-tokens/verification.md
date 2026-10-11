# Verification

## Android — tasks 1.1 (2026-10-10)

Status: done, with the gaps listed under "Not observed". These are client navigation and transport observations only, not storage-page evidence; the read-only DynamoDB failure/success comparison stays in `design.md`.

Ordering: the first attempt found the device behind a secure lock screen (`mobile_diagnose` verdict `blocked`, "Device Locked"; keyguard `showing=true`, `secure=true`), so the task 1.2 codec tests were written before this exploration. They exercise only `ContinuationToken`/`DynamoDbKeySerializer` with JSON strings and SDK key maps and depend on no app path. The exploration ran after the user unlocked the phone.

Setup:

- `adb devices -l`: one device, `ce0417143a6ab8130c` (`SM_G955F`, product `dream2ltexx`, Android 9), state `device`, the device the user chose. `com.felipearpa.fortuna` (installed prod build, signed in) is the app under test.
- `mobile_diagnose(device_serial=ce0417143a6ab8130c)` after unlocking: verdict `ready`.
- Two ARTEMIS Pro runs, both with `device_serial=ce0417143a6ab8130c`, locked to `com.felipearpa.fortuna`, read-only brief (navigate, vertical scroll, pull-to-refresh): trace `9e8de8d5-3c93-44f9-989e-7e437a694834` (all screens) and trace `82d04f47-a89c-4f79-a93c-fb3291cc5dfa` (member management, which the first run did not find). Both ended SUCCESS on the "Mis pollas" home screen; the second run's checker confirmed that no red or "Eliminar polla" item was tapped. The lock screen stayed off throughout.
- Transport: Ktor client logcat captured to a scratch directory outside the repo during both runs. Only paths, methods, and status codes are recorded here, with IDs and hosts removed; no headers, tokens, or bodies are copied. 88 request log lines, all `GET`; 44 responses, all `200`. No request carried a `next` parameter.

Observed screens (Spanish UI; data described generically):

| Screen | Path from launch | Request | Append on scroll | Refresh | End / empty / error |
|---|---|---|---|---|---|
| My pools (gambler scores) | Home screen "Mis pollas" | `GET /gamblers/{id}/pools` | No; about 3 rows fit on screen, no bottom loader | Pull-to-refresh shows no spinner the agent could see, but logcat shows a new `/gamblers/{id}/pools` request each time | Ends after the last row; no marker |
| Open pool layouts | "Mis pollas" → top-right `+` ("Crear polla") → "Polla desde plantilla"; left with back arrow, nothing created | `GET /pool-layouts/open` (also fetched with home) | No; 1 row | Pull issues a new `/pool-layouts/open` request; no visible spinner | No marker |
| Leaderboard | Tap a pool row → pool opens on the "Puntajes" tab | `GET /pools/{id}/gamblers` | No; about 13 rows, no bottom loader | Pull issues a new `/pools/{id}/gamblers` request; no visible spinner | Ends after the last rank |
| Pending bets | Pool → "Apuestas" tab | `GET /pools/{id}/gamblers/{id}/bets/pending` | No rows | Pull issues a new request | Loading placeholders, then the empty state "Nada para mostrar" |
| Finished bets / history | Pool → "Historia" tab | `GET /pools/{id}/gamblers/{id}/bets/finished` (single request) | No; about 64 match cards, grouped by date, all from the first response | No visible spinner, and no second `/bets/finished` request appeared in logcat, so the pull may not have registered | Ends at the earliest match; points summary header |
| Timeline | Pool → "Puntajes" → tap a participant row → "Línea de tiempo" | `GET /pools/{id}/gamblers/{id}/bets/timeline` (single request) | No; about 64 cards | Pull issues a new `/bets/timeline` request; no visible spinner | Tournament is finished, so only finished entries appeared; the live-to-finished transition was not observable |
| Ranked match bets | Pool → "Historia" → tap a match card → "Marcador" | `GET /pools/{id}/matches/{id}/bets?sort=pool-score-desc` | No; about 13 rows ordered by standings | Pull issues new match and ranked-bets requests | Ends after the last member |
| Member management | "Mis pollas" → a pool the account owns (the second one) → drawer (top-left) → "Jugadores" | `GET /pools/{id}/members` | No; 1 row | Pull issues a new `/pools/{id}/members` request; no visible spinner | No title text; "Editar" action at top right (not tapped) |

The agents left screens with the in-app back arrow and relaunched the app when they needed to return to "Mis pollas"; no BACK-exit recovery was needed.

Not observed:

- No list crossed a page boundary: every list loaded in one response and no `next` token was sent, so append, the Timeline live-to-finished transition, and invalid-token recovery were not exercised on this account's data. Task 5.1 needs a fixture with more rows than a page.
- Live/unsettled bets: the Android app has no separate live screen. `GetLivePoolGamblerBets` is wired in DI but no screen calls it; live entries reach the client only through Timeline, and no match was live.
- Legacy match bets: the Android client always sends `sort=pool-score-desc` for match bets, so the legacy (unsorted) endpoint cannot be reached from the app.
- Pull-to-refresh indicator: the agent saw no spinner because each refresh returned within a few hundred milliseconds. The source uses a Material 3 `PullToRefreshBox` via `RefreshableLazyPagingColumn`, and the refetch requests above confirm the gesture worked on every screen except History.
- No error or retry state appeared; no failure was induced.

Accepted without multi-page append, the Timeline live-to-finished transition and live phase, invalid-token recovery, the legacy match-bets endpoint, History pull-to-refresh, and error/retry states by the user on 2026-10-10.

## Backend — tasks 1.2–1.3 (2026-10-10)

These results cover the shared codec only. They are not storage-page evidence; no DynamoDB service or fixture was queried. The read-only service comparison remains in `design.md`.

Commands, run from `Amazon.Lambda/`:

| Command | Result |
|---|---|
| `dotnet test Felipearpa.Data.DynamoDb/test` | Passed: 82, failed: 0 |
| `dotnet build Felipearpa.Tyche.slnx` | Succeeded, 0 errors; warnings only in files this change did not touch |
| `dotnet test Felipearpa.Tyche.Pool/test --no-build` | Passed: 184, failed: 0 |
| `dotnet test Felipearpa.Tyche.PoolLayout/test --no-build` | Passed: 7, failed: 0 |

Codec coverage in `Felipearpa.Data.DynamoDb/test/KeySerializerTests.fs`, all using the real codec and `System.Text.Json` (no mocked serializer):

- Version 1 round trips compare each attribute's name, S/N/B type, and value for: leaderboard (`pk`, `sk`, numeric `score`), pending bets (`matchDateTime`), fan-out composite keys, open layouts (`status`, `startDateTime`), a username with Unicode, emoji, quotes, backslash, and `+&=/?#%`, numbers `0`, `-7`, a 38-significant-digit decimal, `-0.000000000000000000000000000001`, `1E+125`, `120.50` (text kept, not normalized), and binary bytes including `00` and `FF`.
- The documented examples in `docs/backend-pagination.md` are asserted verbatim: the encoded leaderboard token, both member orders of the four-attribute decode example, the legacy decode example, and the legacy writer's output.
- Legacy `{"pk","sk"}` tokens decode as two string attributes; the serializer reads version 1 tokens while it still writes legacy tokens.
- Rejected tokens: 22 malformed envelopes (empty, blank, not JSON, non-object, `v` as string or `1.5`, missing `key`, `key` as array or empty, extra members, duplicate members, multi-type attributes, truncated JSON, incomplete or extended legacy shapes) plus `null`; versions `0`, `2`, `-1`; types `SS`, `NS`, `BS`, `M`, `L`, `BOOL`, `NULL`, and lowercase `s`; 18 invalid values (non-string, empty, malformed, signed, padded, `NaN`, `Infinity`, hex, 39 significant digits, lone surrogate, bad or empty base64); legacy tokens with empty or non-string values. `Deserialize` raises `InvalidContinuationException` carrying the case.
- Completion: `null` and empty `LastEvaluatedKey` produce no `next`; a nonempty key produces a token; `Serialize` refuses an empty key; a non-scalar attribute from DynamoDB is reported as invalid service state.

## Backend — tasks 2.1–2.5 (2026-10-10)

These results come from local unit tests against the in-memory DynamoDB fake (`Felipearpa.Tyche.Pool/test/Fakes`) and scripted SDK responses. They are not service evidence: no DynamoDB service, AWS account, or device was used. Service-backed traversal stays with task 3.2.

Commands, run from `Amazon.Lambda/`:

| Command | Result |
|---|---|
| `dotnet build Felipearpa.Tyche.slnx` | Succeeded, 0 errors; the warnings are in files this change did not touch, plus existing warnings in `FakeDynamoDb.fs` |
| `dotnet test Felipearpa.Data.DynamoDb/test --no-build` | Passed: 93, failed: 0 (82 before; 11 new contract tests) |
| `dotnet test Felipearpa.Tyche.Pool/test --no-build` | Passed: 253, failed: 0 (184 before; 69 new) |
| `dotnet test Felipearpa.Tyche.PoolLayout/test --no-build` | Passed: 13, failed: 0 (7 before; 6 new) |
| `dotnet test Felipearpa.Tyche.AmazonLambda/test --no-build` | Passed: 86, failed: 0 (regression control) |
| `dotnet test Felipearpa.Tyche.Function/test --no-build` | Passed: 1, failed: 0 (regression control) |

Writer format: the deployed `DynamoDbKeySerializer` still writes legacy tokens, as the reader revision requires. Tests that follow tokens across index pages use `VersionOneKeySerializer` (`Felipearpa.Tyche.Pool/test/Infrastructure/PaginationSupport.fs`, and a copy in the PoolLayout test), which writes `ContinuationToken.encode` and reads through `DynamoDbKeySerializer`. The same tests run with the deployed serializer show that its index tokens are rejected with `MissingKeyAttribute` before any query, and that its layout-match tokens traverse every page.

Storage pages: `FakeDynamoDb.LimitQueryPages` caps each query at two evaluated items (one for the application tests) without changing the request the repository builds; production requests still have no `Limit`. The fake now rejects a start key whose attribute names differ from the table and index keys, as DynamoDB does, records every query request, and can end a query with an empty key map. It does not check key value types against a schema.

Mutation check: when `StartKeyContract.startKey` was temporarily changed to keep only `pk` and `sk`, 28 of the repository pagination tests failed; the application tests had not been added yet. The failures were every index operation's traversal, empty-final-key, and retry case plus the leaderboard score check; layout matches still passed. The change was reverted before the runs above.

2.1 to 2.4 — `Infrastructure/SharedCodecPaginationTest.fs`, one case per operation (pending, live, finished, and legacy match bets; gambler pool scores; pool leaderboard; pool members; gamblers by layout; open layouts; layout matches):

- Each traversal crosses three storage boundaries and returns the expected pages. The first request has no start key, and each later request carries the exact names, S/N types, and values of the item that ended the previous storage page.
- Filtered pages that are empty but still have a key keep their token and continue: pending and live (computed bets), finished (search text), legacy match bets (bets not yet locked), gambler scores and leaderboard (closed memberships), layout matches (version 2 matches). Pool members, gamblers by layout, and open layouts have no filter. For open layouts, an empty page that still has a key is covered with scripted responses in `PoolLayoutPaginationTest.fs`.
- Ties across a boundary: match dates (pending, live), settlement dates (finished), score 5 (gambler scores, leaderboard), username "bea" (members), start date (open layouts). A page boundary falls on score `0`, and the leaderboard token decodes `score` as `N` with the values `5`, `5`, `0`.
- An absent final key and an empty final map both end with no `next`.
- A request with page 1's token fails once and is retried twice. All three requests carry the same start key, and both retries return the same items and the same `next`.
- Invalid keys are rejected before any query: missing or string `score` (gambler scores, leaderboard), missing date key (live given a finished key, finished given a live key), extra attribute, numeric username, and another gambler, pool, match, layout, `status`, or `sk` prefix. Legacy `pk`/`sk` tokens are rejected for all nine index operations and accepted for layout matches, both through the fake and against scripted responses.

2.5 — `Application/ContinuationTraversalTest.fs`:

- Timeline over two-item pages returns pages of `[B004]`, `[]`, `[B001, B005, B003]`, and `[B002]`. The empty live page keeps a `live:` token. The switch to finished entries happens only when the live phase has no key, and the `finished:` token carries `pk`, `sk`, and `computedDateTime`. The last page has no token.
- An unprefixed live inner token gives the same page and `next` as its `live:` form. An unprefixed finished inner token is read as live and rejected with `MissingKeyAttribute "matchDateTime"`.
- Added-match fan-out over one-item gambler pages reaches all three members and requests the reconciliation once. Its gamblers-by-layout requests start at none, then member A's index key, then member B's.
- A later gambler page that fails leaves the fan-out failed: member A has a detail, member C has none, and no reconciliation is requested. Redelivery completes every member and requests the reconciliation. With the deployed legacy writer, fan-out over several pages fails with `MissingKeyAttribute "getGamblersByPoolLayoutPk"` and does not complete.
- Joined-gambler materialization over one-item layout-match pages reaches all three matches with either writer. When a later layout-match page fails, the materialization fails after the first match, and a retry creates the rest.

Not verified here: HTTP `INVALID_CURSOR` responses (task 3.1), traversal against the DynamoDB service and its key-type validation (task 3.2), and reader/writer rollout combinations (task 3.3).

## Backend — tasks 3.1–3.3 (2026-10-10)

The writer revision is not applied: the working tree is the reader revision (`DynamoDbKeySerializer.Serialize` still calls `ContinuationToken.encodeLegacy`). The service fixture uses a test writer that emits version 1 tokens, as the writer revision will. No deployment, Lambda, queue, or production table was changed or queried. The work ran on 2026-10-11 after two interrupted sessions; no AWS resource from those sessions existed when it resumed.

Commands, run from `Amazon.Lambda/` with .NET SDK 10.0.201:

| Command | Result |
|---|---|
| `dotnet build Felipearpa.Tyche.slnx` | Succeeded, 0 errors; builds both HTTP hosts (`Felipearpa.Tyche.HttpApi`, `Felipearpa.Tyche.AmazonLambda`) and the verification harness |
| `dotnet test Felipearpa.Data.DynamoDb/test --no-build` | Passed: 99, failed: 0 (93 before; 6 rollout combinations) |
| `dotnet test Felipearpa.Tyche.Pool/test --no-build` | Passed: 253, failed: 0 |
| `dotnet test Felipearpa.Tyche.PoolLayout/test --no-build` | Passed: 13, failed: 0 |
| `dotnet test Felipearpa.Tyche.AmazonLambda/test --no-build` | Passed: 113, failed: 0 (86 before; 27 invalid-cursor) |
| `dotnet test Felipearpa.Tyche.Function/test --no-build` | Passed: 1, failed: 0 |
| `dotnet test Felipearpa.Tyche.HttpApi/test --no-build` | Passed: 27, failed: 0 (new project) |

### 3.1 — `INVALID_CURSOR` in both hosts

`ContinuationResponse.orInvalidCursor` (`Felipearpa.Tyche.Function`) wraps the nine shared-codec endpoint functions that both hosts call. It maps only `InvalidContinuationException` to 400 with `{"error":"INVALID_CURSOR","message":"The continuation is not valid; restart from the first page"}`, the body the ranked feed already uses for its own invalid cursor. The ranked endpoint and its cursor are unchanged. Recovery behavior is documented in `docs/backend-pagination.md` ("Invalid cursor responses").

Each host has three theories over the nine endpoints (leaderboard, pending, live, finished, timeline, legacy match bets, gambler pools, pool members, open layouts):

- Lambda: `Felipearpa.Tyche.AmazonLambda/test/Function/InvalidCursorTest.fs` calls the Lambda handlers with a mocked DynamoDB client.
- HttpApi: `Felipearpa.Tyche.HttpApi/test/InvalidCursorTest.fs` hosts the real routers on an ASP.NET test server with the production dependency registrations. Only DynamoDB, the account lookup, and authentication are replaced: a test scheme authenticates the caller in place of the Firebase JWT. Tokens go URL-encoded in `?next=`.

1. Unusable tokens: malformed text, version 2, legacy `pk`/`sk` without the index keys, `score` as a string, a live key sent to finished and a finished key sent unprefixed to the timeline, and tokens for another pool, gambler, match, or a `CLOSED` layout. Each gets exactly that 400 body, and no query reaches an index. For pool members, the base-table pool lookup and ownership check still run first.
2. A usable version 1 token gets 200. The Lambda test checks that the token's key becomes the index query's exact start key; the HttpApi test checks that the index query ran with a start key.
3. A DynamoDB failure for a usable token is not turned into `INVALID_CURSOR`. The `AmazonDynamoDBException` propagates out of the handler as before; Kestrel and API Gateway answer that with 500, and the test server surfaces it to the caller.

Mutation check: when `orInvalidCursor` was temporarily changed to catch nothing, the 9 Lambda unusable-token theories failed (`Failed: 9, Passed: 104`). The change was reverted before the runs above.

### 3.2 — Isolated DynamoDB fixture

Environment: account 567776770335, `us-east-2`, run from the development Mac with the default AWS CLI profile. Commands:

```bash
cd Amazon.Lambda/operations
AWS_REGION=us-east-2 ./pool-table.sh create-copy Pool-verify-20261011 PoolLayout-verify-20261011
cd ../Felipearpa.Tyche.Verification
POOL_TABLE_NAME=Pool-verify-20261011 POOL_LAYOUT_TABLE_NAME=PoolLayout-verify-20261011 AWS_REGION=us-east-2 \
  dotnet run -- pagination <scratch>/pagination-results.json
aws dynamodb delete-table --region us-east-2 --table-name Pool-verify-20261011
aws dynamodb delete-table --region us-east-2 --table-name PoolLayout-verify-20261011
aws dynamodb wait table-not-exists ...   # both tables
aws dynamodb list-tables --region us-east-2
```

- Schema: `create-copy` now also gives the layout copy `GetOpenedPoolLayout-index` (`status`/`startDateTime`, projection ALL). Before that change, the copy had no index, so it did not match production. The production schemas were read with `describe-table`, metadata only. The Pool copy has the same eight indexes, keys, and projections as `Pool` (the seven shared-codec indexes plus the ranking index). All indexes were `ACTIVE` before the run.
- Fixture: the `pagination` mode in `Felipearpa.Tyche.Verification/Pagination.fs` seeds 39 Pool items and 20 PoolLayout items under fresh random IDs. Pool items: 8 members of one pool, 6 other memberships of gambler 0, 12 bets of gambler 0, 7 other bets on the first match, and 6 bets of gambler 0 in the other pools. PoolLayout items: 7 open layouts, 1 closed layout, and 12 layout matches.
- Synthetic data: two legacy match bets have a future kickoff on a match whose other bets are past, so that the lock filter empties one page. Real data has one kickoff per match.
- Paging: queries without their own `Limit` get `Limit = 2` from the fixture's client only.
- Convergence: each operation polled its full traversal, with a 3-minute deadline, until it returned the expected items. All converged on the first attempt, in 0.4 to 0.8 s.
- Results: exit code 0, 55 of 55 checks passed (fixture pool `01M4NKJCZTGJ6J6MFRM0XCM12R`).

| Operation | Storage pages (items per page) | Token key | Filtered-empty page | Legacy writer's token / pk-sk start key sent to DynamoDB |
|---|---|---|---|---|
| Pending bets | 6 (2,0,2,0,0,2) | `matchDateTime` S, `pk`, `sk` | Yes | `MissingKeyAttribute matchDateTime` / "The provided starting key is invalid" |
| Live bets | 6 (2,0,0,2,0,2) | same | Yes | same |
| Finished bets | 4 (2,2,2,0) | `computedDateTime` S, `pk`, `sk` | — | `MissingKeyAttribute computedDateTime` / invalid |
| Finished bets, search text | 4 (2,0,2,0) | same | Yes | same |
| Legacy match bets | 4 (2,0,2,2) | both `getPoolGamblerScoresByMatch*` S, `pk`, `sk` | Yes | `MissingKeyAttribute getPoolGamblerScoresByMatchPk` / invalid |
| Gambler pool scores | 4 (2,0,2,1) | `score` N, `pk`, `sk` | Yes | `MissingKeyAttribute score` / invalid |
| Pool leaderboard | 5 (2,2,0,2,0) | `score` N, `pk`, `sk` | Yes | same |
| Pool members | 4 (2,2,2,2) | `gamblerUsername` S, `pk`, `sk` | — (no filter) | `MissingKeyAttribute gamblerUsername` / invalid |
| Gamblers by layout (fan-out) | 8 (2,2,2,2,2,2,2,0) | both `getGamblersByPoolLayout*` S, `pk`, `sk` | — (no filter) | `MissingKeyAttribute getGamblersByPoolLayoutPk` / invalid |
| Open layouts | 4 (2,2,2,1) | `status`, `startDateTime` S, `pk`, `sk` | — (no filter) | `MissingKeyAttribute startDateTime` / invalid |
| Layout matches (base table) | 6 (2,0,2,2,2,2) | `pk`, `sk` | Yes | Legacy tokens traverse all 6 pages / accepted |

For every row, the version 1 traversal met all of these checks:

- It returned every expected item exactly once.
- The last page had no token, whether that page was empty or held the last items.
- Every token decoded and fit the query's start-key contract.
- Every token was unchanged after URL encoding into `?next=` and decoding with `HttpUtility.ParseQueryString`.
- The index sort values in successive tokens were in query order: ascending, descending, or numeric descending.
- Following the same tokens a second time returned identical pages and tokens.

The fixture also placed ties across page boundaries: usernames (`bea` ×3), scores (5 ×3 on the leaderboard, with score `0` on the last page), and open-layout start dates. Members included the non-ASCII usernames `émile` and `zoë`.

Timeline and regression controls:

- Timeline (`GetGamblerBetsTimeline` over the repository): 9 pages, items 2,0,0,2,0,4,2,2,0. The tokens were `live:` ×5, then `finished:` ×3, then none. Empty live pages kept `live:`, every live entry came before every finished entry, and the page where live ended carried the first finished entries.
- Ranked match bets (`RankedMatchBetsCursor`, limit 2): 4 pages; all 8 participants in total order.
- Settlement match-index discovery (raw-key cursor): 8 pages; all 7 pools with the match found.
- Username propagation (raw-key queries over the scores-by-gambler index and bet keys, 2-item pages): updated 25 of 25 items (7 memberships, 18 bets), read back with consistent reads. It ran last because it renames gambler 0.

Cleanup: both tables were deleted and `wait table-not-exists` returned for each. `list-tables` afterwards shows only `Account`, `MatchScoreIngestion`, `Pool`, and `PoolLayout`. No other resource was created.

### 3.3 — Two-revision rollout

`docs/backend-pagination.md` ("Rollout") now holds the deployment instructions:

- Deploy the reader revision everywhere, then the writer revision right after it.
- Until the writer revision, index lists longer than one storage page get `INVALID_CURSOR` on their second page, and fan-out over more than one gambler page keeps failing and retrying.
- Legacy base-table tokens keep working; legacy index tokens need a refresh.
- Roll back only to the reader revision.
- Replaying failed fan-out uses the existing runbook procedure: retained stream failure, then `RECONCILE_POOL` for each of the layout's pools, after the writer revision.

`Felipearpa.Data.DynamoDb/test/RolloutCompatibilityTest.fs` checks each reader/writer combination. "Original" is a test copy of the serializer from the last commit.

| Writer → reader | Result |
|---|---|
| Reader revision → (wire format) | Byte-identical to the original serializer's output for index and base-table keys |
| Reader revision ↔ original | Each reads the other's base-table tokens, so a mixed fleet during the reader deployment is safe |
| Original (legacy) base-table token → reader and writer revisions | Accepted with the same key |
| Original (legacy) index token → reader and writer revisions | `MissingKeyAttribute "score"`; HTTP gives `INVALID_CURSOR` and the client refreshes |
| Writer revision → reader revision (also the rollback target) | Decodes the full key, including numeric `score`, and fits the contract |
| Writer revision → original | `KeyNotFoundException` (a raw server error, not `INVALID_CURSOR`), so no instance may run the original serializer once version 1 tokens exist |

The service fixture adds the same combinations against DynamoDB: version 1 tokens traverse every operation, the reader revision's own legacy tokens are rejected for index queries and work for layout matches, and DynamoDB itself rejects the pk/sk start key for every index.

Not verified:

- The deployed rollout itself. Neither revision was deployed, so the reader-then-writer sequence, the absence of new invalid-key errors after the writer revision, and a real fan-out replay were not observed. The replay procedure is recorded but was not run.
- HttpApi under Kestrel with the real Firebase JWT bearer scheme. The test server replaces authentication, and it surfaces unhandled exceptions instead of a 500 response, so the 500 for a DynamoDB failure is not observed directly.
- An API Gateway round trip of a token. URL transport was checked with `Uri.EscapeDataString` and `HttpUtility.ParseQueryString`, and the Lambda tests pass tokens as already-decoded query parameters, as API Gateway does.
- The service fixture used repositories and the Timeline application service, not the HTTP handlers, and storage pages forced by `Limit = 2`, not 1 MB pages.

Accepted without the HttpApi run under Kestrel with a real Firebase JWT, a directly observed 500 for unrelated failures (3.1), HTTP-handler traversal and 1 MB storage pages in the service fixture (3.2), and a real two-revision deployment and fan-out replay (3.3) by the user on 2026-10-11.

## iOS — task 4.1 (2026-10-11)

Status: done, with the client limitations and gaps listed below. The existing iOS client carries version 1 tokens unchanged, appends and refreshes every affected list it can reach, shows Timeline's live-to-finished switch, and recovers from a rejected legacy index token with an ordinary pull-to-refresh. The ranked feed still uses its own cursor. The run also exposed two client problems that predate this change; see "Client limitations".

### Environment

- Tables: `Pool-verify-20261011` and `PoolLayout-verify-20261011` in account 567776770335, `us-east-2`, created with `AWS_REGION=us-east-2 ./pool-table.sh create-copy Pool-verify-20261011 PoolLayout-verify-20261011`. They are kept for task 5.1. No production `Pool` or `PoolLayout` item was read or written, and nothing was deployed.
- Host: the repository's HttpApi, run locally from a scratch project outside the repository. The project calls the unchanged `RegisterServices`, `ConfigureMiddleware`, and `Configure*Routes`, then replaces three registrations for this host only:
  - `IKeySerializer`: writes version 1 tokens with `ContinuationToken.encode` (the writer revision) and reads through `DynamoDbKeySerializer`. While a flag file exists, it writes with the committed `DynamoDbKeySerializer` (legacy `{pk,sk}`), which is the reader revision.
  - `IAmazonDynamoDB`: a client that sets `Limit` only on queries to the eight affected indexes that set none of their own. Base-table, ranked-index, and account queries are unchanged.
  - `RankedMatchBetsSettings`: page size 4, so the ranked feed crosses pages.

  A request log recorded method, path, status, item count, and only the shape of `next` in and out, never header values. No file in the repository was edited: `git status` and `git diff` show no iOS or HttpApi change, and `DynamoDbKeySerializer.Serialize` still calls `ContinuationToken.encodeLegacy`.
- Caller: the signed-in account was resolved the usual way, by a read-only lookup by email in the production `Account` table. The account ID came from the host's request log of `GET /gamblers/{id}/pools` and is not recorded here.
- App: scheme `Tyche Local` built from `Tyche.xcworkspace` with `xcodebuild ... 'URL_BASE_PATH=http:/$()/localhost:5098' build`. The command-line setting overrides `AppConfigLocal.xcconfig` (its ngrok URL) without editing it. The app ran on the iPhone 16 Pro (iOS 18.1) simulator `A2F5EF6F-BA25-4035-9494-F6ACE0144047`, which was already signed in. No physical device was used.
- Synthetic data, all under random IDs: a main pool owned by the signed-in account with 15 members (scores tied at 27, 21, 15, and 0), 11 other memberships of the account, and 36 bets for the account in the main pool. The bets cover 14 settled matches, 7 matches locked without a result ("live", interleaved with the settled ones), and 15 upcoming matches. Every member has a ranked detail on the two oldest settled matches. The PoolLayout table has 10 open layouts, 1 closed layout, and 36 layout matches. During the run, member "dani" got the same 34 other bets so a participant Timeline has both phases. The layout start times were rounded to whole seconds: with fractional seconds, the open-layouts screen showed "Network error", because the client's `.iso8601` date decoding rejects fractions. That is a data artifact of the first seeding, not a pagination failure.
- Page limits: 3, then 10, 20, and 4 at different points; a control run used 1000 (no split).

### Results

Across the run, 88 requests carried `next`. Every one was byte-identical to a token the host had issued earlier in the same log, so URL encoding, the `+` and `:` in date keys, and the `live:` prefix all survived the client. First pages sent no `next` parameter (Alamofire drops the `nil` value), so the old literal `next=nil` did not occur. Every page request returned 200, except the 5 deliberate legacy appends.

| List (screen) | Endpoint | Token shape sent and returned | Pages observed | Client result |
|---|---|---|---|---|
| My pools | `GET /gamblers/{id}/pools` | `v1{pk:S,score:N,sk:S}` | Limit 3: 3+3+3, then stalled (see limitations). Limit 10: 10+2 | All 12 pools shown at limit 10, then blank space below |
| Leaderboard (Scores) | `GET /pools/{id}/gamblers` | `v1{pk:S,score:N,sk:S}` | 10+5 | All 15 in score order (ties as stored); rows 11 and 12 also appeared again after row 14 |
| Pending (Bets) | `GET /pools/{id}/gamblers/{id}/bets/pending` | `v1{matchDateTime:S,pk:S,sk:S}` | Limit 10: 3+3+10, never asked for page 4. Limit 20 after refresh: 6+16 | Limit 20: all 22 unsettled bets, the last on 26/10 |
| History | `GET .../bets/finished` | `v1{computedDateTime:S,pk:S,sk:S}` | 10+4, one append | All 14 settled bets, the last on 20 Sep |
| Timeline (dani) | `GET .../bets/timeline` | `live:v1{matchDateTime:S,pk:S,sk:S}` | Limit 20: 7 live entries with a `live:` token, then one page of 14 finished entries without `next` | Live entries ("Points pending") followed by settled entries in finished order |
| Members (Gamblers, owner) | `GET /pools/{id}/members` | `v1{gamblerUsername:S,pk:S,sk:S}` | 10+5, one append, after a pull-to-refresh | All 15 alphabetically |
| Open layouts ("Pool from layout") | `GET /pool-layouts/open` | `v1{pk:S,sk:S,startDateTime:S,status:S}` | Limit 4: 4+4+2 | Only 8 of 10 shown |
| Ranked match bets (Score) | `GET /pools/{id}/matches/{id}/bets?sort=pool-score-desc` | Ranked cursor (opaque, 374 characters, not the version 1 envelope) | 4+4+4+3 | 15 participants in total order |

Timeline at limit 10 asked only for the first two live pages (4 and 3 entries) and then stopped, for the client reason below, before the switch to finished entries. At limit 20, the request with the second `live:` token returned the finished entries, so the switch happened inside one response, as in task 3.2. No `finished:` token reached the client in this run: all 14 finished entries fit in one page.

Rejected legacy token and recovery, on the Bets tab at limit 20:

1. With the legacy writer on (reader revision), the first page returned 6 bets and a `legacy{pk,sk}` token. The client's appends with that token got 400 `INVALID_CURSOR` in 1 to 7 ms, without an index query (5 concurrent requests, all 400). The list showed the 6 bets and then ended, with no error row or retry control at the end of the list.
2. With the version 1 writer back on (writer revision), one pull-to-refresh sent a first page without `next`. It returned 6 bets and a `v1{matchDateTime:S,pk:S,sk:S}` token, and one append returned the other 16 with no `next`. The list then showed all 22 bets.

### Client limitations

These predate this change and do not depend on the token format.

- Concurrent duplicate appends. When the last rows of a page appear together, `lazy-paging-swift` 0.0.3 sends one append per row (3 to 5 identical requests with the same token). `LazyPagingItems.append()` checks `isLoading` before it awaits `pageFetcher.canAppend()`, and all callers pass the check before any sets it. Every copy of the page is appended. The effects were repeated rows (leaderboard), blank scroll space (My pools), missing rows (open layouts: 8 of 10), and stalled appends (pending at limit 10, Timeline at limit 10, My pools at limit 3). Stalls happen because duplicates make `loadedItems` longer than the rows SwiftUI renders, so no rendered row reaches the prefetch threshold again. The backend answered every duplicate identically, as the retry requirement says. With production storage pages (up to 1 MB, no `Limit`), lists rarely have a second page, so this has stayed hidden.
- Refresh request storm. After a screen is pushed over the pool home (match Score, Gamblers), the client requests the leaderboard first page continuously, and the pending first page too once the Bets tab has been opened. It made about 110 requests per second (7,683 leaderboard and 3,622 pending first pages in total) until the app was terminated, and going back to the tab did not stop it. The control run with no forced page split (limit 1000) showed the same storm, 568 leaderboard requests in about 6 seconds, so pagination does not cause it. It was not root-caused.
- Rows that are already visible do not reappear after a pull-to-refresh. At limit 3, a refreshed first page of 3 rows that fit on screen triggered no append, so the list stayed at 3 rows.
- Empty nonfinal pages were avoided by the data design, because the client appends only when a row appears. A page with no items and a `next` would leave nothing new to appear. This was not exercised.

### Not observed

- Legacy match bets and a separate live-bets list: neither is reachable from the iOS app. The ranked feed replaced the match list, and live entries appear only in Timeline.
- A `finished:` token carried by the client (above), and an empty nonfinal page on the client.
- A legacy token held from an older backend while the app stays open: the legacy token here came from the reader-revision writer on the same host, then the writer was switched before the refresh.
- Production-sized storage pages: pages were forced with small `Limit` values on the affected indexes.
- Dark mode, large text, and other presentation conditions: the presentation is unchanged, so they were not checked.

Cleanup: the local host was stopped, the app was terminated, and the simulator was shut down. The Tyche app on that simulator is now the `Tyche Local` build pointed at `http://localhost:5098`. Both verify tables remain for task 5.1.

Accepted without legacy match bets and a separate live list (unreachable from iOS), a `finished:` token reaching the client, an empty nonfinal page on the client, an older-backend token across a real deployment, and 1 MB storage pages by the user on 2026-10-11.

## Android — task 5.1 (2026-10-11)

Status: done, with the gaps under "Not observed". ARTEMIS stopped partway through the run, so the rest of the screens were driven with plain `adb` input; see "Environment". The existing Android client carries version 1 tokens unchanged and appends and refreshes every affected list it can reach. It follows Timeline from live to finished entries, including a `finished:` token. After the backend rejects a legacy index token, an ordinary pull-to-refresh restores the full list. The ranked feed still uses its own cursor. The Android client showed neither of the iOS client problems from task 4.1 (duplicate appends, request storm).

### Environment

- Device: the Android emulator `emulator-5554` (AVD `Pixel_7_API_36_no_account`, Android 16), chosen by the user and booted with `mobile_diagnose(launch_avd=...)`. The Galaxy S8 was not used. ARTEMIS installed its accessibility helper on the emulator (`mobile_diagnose(attempt_fix=true)`), and the final verdict was `ready`. The UI language is English.
- App: the Local flavor, built with `./gradlew :app:assembleLocalDebug -PurlBasePathLocal=http://localhost:5098` and installed with `adb install -r`. The Gradle property overrides the ngrok URL in `gradle.properties` without editing it. The Local manifest already allows cleartext traffic. `adb reverse tcp:5098 tcp:5098` forwarded the emulator's `localhost:5098` to the host. The user signed in on the emulator. The account matched the one seeded for task 4.1, so nothing was seeded again.
- Backend: the same local HttpApi host, tables, synthetic data and request log as task 4.1. Page limit 10 for the first part, then 4 (the host was restarted). The ranked page size was 4 throughout. Sign-in sent `POST /accounts` (200), the ordinary account-link update. The host does not redirect the Account table, so this updated the user's existing row in the production `Account` table, as any sign-in does. No production `Pool` or `PoolLayout` item was read or written.
- Driver:
  - ARTEMIS Pro run `7dc34b87-17f5-467e-93ee-0f938afb1cbe` (locked to `com.felipearpa.fortuna`, read-only brief) covered My pools, the leaderboard, and the first pending-bets traversal. It then stopped making progress because its Gemini key reported `402 Payment Required` (prepaid credits depleted), and the run was stopped.
  - The remaining screens were driven with `adb shell input` (tap, swipe, pull-to-refresh), using the paths from task 1.1. They were read through `uiautomator dump` accessibility text and screenshots kept outside the repository.
  - No bet, member, pool or account action was taken. "Delete pool", "Edit" and "Log out" were not tapped.

### Results

The host logged 138 requests: 137 `GET` and 1 `POST`, with 135 responses of 200, 2 of 400 (the deliberate legacy appends) and 1 of 401 (before sign-in). 67 requests carried `next`, and every one was byte-identical to a token the host had issued earlier, including the `live:` and `finished:` prefixes. Only one `next` was ever re-sent without a first-page request in between: the deliberate Retry below. First pages sent no `next` parameter.

| List (screen) | Endpoint | Token shape sent and returned | Pages observed | Client result |
|---|---|---|---|---|
| My pools | `GET /gamblers/{id}/pools` | `v1{pk:S,score:N,sk:S}` | Limit 10: 10+2. Limit 4: 4+4+4 | All 12 pools, no duplicates, before and after refresh |
| Leaderboard (Scores) | `GET /pools/{id}/gamblers` | `v1{pk:S,score:N,sk:S}` | Limit 10: 10+5 (again after refresh). Limit 4: 4+4+4+3 | All 15 in score order, ties as stored, no duplicates |
| Pending (Bets) | `GET /pools/{id}/gamblers/{id}/bets/pending` | `v1{matchDateTime:S,pk:S,sk:S}` | Limit 10: 3+3+10+6, again after each refresh | All 22 unsettled bets, the last on 10/26/26 |
| History | `GET .../bets/finished` | `v1{computedDateTime:S,pk:S,sk:S}` | 10+4; after refresh 10, then +4 when scrolled to the end | Settled bets down to the earliest, Sep 20 |
| Timeline (dani) | `GET .../bets/timeline` | `live:v1{matchDateTime:S,pk:S,sk:S}`, then `finished:v1{computedDateTime:S,pk:S,sk:S}` | Limit 10: live 4, live 3, then 10 finished with a `finished:` token, then finished 4 with no `next`. Same after refresh | 7 "Points pending" entries first, then 14 settled entries down to Sep 20 |
| Members (Gamblers, owner) | `GET /pools/{id}/members` | `v1{gamblerUsername:S,pk:S,sk:S}` | 10+5, again after each refresh | All 15 alphabetically, no duplicates |
| Open layouts ("Choose a template") | `GET /pool-layouts/open` | `v1{pk:S,sk:S,startDateTime:S,status:S}` | Limit 10: 10, no `next`. Limit 4: 4+4+2, again after refresh | All 10 layouts (iOS showed 8 of 10) |
| Ranked match bets (oldest settled match) | `GET /pools/{id}/matches/{id}/bets?sort=pool-score-desc` | Ranked cursor (opaque, 374 characters, not the version 1 envelope) | 4+4+4+3, again after refresh | 15 participants in standings order |

Timeline's switch from live to finished entries happened inside the response to the second `live:` token, as in tasks 3.2 and 4.1. The client then carried the `finished:` token to the last page, which task 4.1 did not observe. During two quick refreshes on Timeline, two in-flight `live:` appends ended without a response body. The log shows them as `items=-`, consistent with the client cancelling them for the refresh, and the following traversal completed normally.

Rejected legacy token and recovery, on the Bets tab at limit 10:

1. With the legacy writer on (reader revision), a pull-to-refresh returned 3 bets and a `legacy{pk,sk}` token. The single append with that token got 400 `INVALID_CURSOR` in 9 ms. The list showed the 3 bets, then an inline "Unexpected error" message with a Retry button. Tapping Retry re-sent the same legacy token and got 400 `INVALID_CURSOR` again in 1 ms, so Retry does not recover.
2. With the version 1 writer back on (writer revision), one pull-to-refresh sent a first page without `next`. It returned 3+3+10+6 bets with `v1{matchDateTime:S,pk:S,sk:S}` tokens and ended with no `next`. The error row disappeared and the list showed all 22 bets, the last on 10/26/26.

### Client observations

- No duplicate appends: each page boundary produced one request per traversal. The leaderboard, open layouts and My pools showed every row exactly once.
- No request storm: the log grew only when a screen was opened, scrolled or refreshed. The busiest second came from back-to-back pull-to-refresh gestures by the driver.
- The append error row shows the generic "Unexpected error" text for `INVALID_CURSOR`, and its Retry re-sends the rejected token. Recovery needs a pull-to-refresh, as the design expects; no automatic restart is required. This is existing client behavior and was not changed.
- A refresh re-traverses pages until rows fill the screen, so a refresh can issue several pages at once (pending: 4 requests).
- One "Fortuna isn't responding" dialog appeared before sign-in, on the first launch right after install (`Input dispatching timed out`, no focused window). It was dismissed with "Wait", did not recur, and the crash buffer has no Fortuna entries.

### Not observed

- Legacy match bets and a separate live-bets list: neither is reachable from the Android app (task 1.1).
- An empty nonfinal page on the client: the data design avoided it.
- A legacy token held from an older backend while the app stays open: as in task 4.1, the legacy token came from the reader-revision writer on the same host before the writer was switched back.
- Production-sized storage pages: pages were forced with `Limit` 10 and 4 on the affected indexes.
- ARTEMIS coverage after the first pending-bets traversal: those screens were driven with `adb` because ARTEMIS's LLM credits ran out.
- Presentation conditions (dark mode, large text, other widths): the presentation is unchanged, so they were not checked.

Accepted without ARTEMIS coverage after the first pending-bets traversal (adb-driven instead, after the ARTEMIS LLM key returned 402), legacy match bets and a separate live list (unreachable from Android), an empty nonfinal page on the client, an older-backend token across a real deployment, and 1 MB storage pages by the user on 2026-10-11.

## Backend — task 3.4 (2026-10-11)

`DynamoDbKeySerializer.Serialize` now calls `ContinuationToken.encode`, so the backend writes version 1 tokens; decoding of both formats and the start-key contracts are unchanged. `ContinuationToken.encodeLegacy` was removed from the source because no production path used it. Tests and the verification harness that need a legacy token now write `{"pk":"...","sk":"..."}` themselves, as the earlier backend did. No deployment, Lambda, queue, or AWS table was changed or queried, and the `pagination` service fixture was not re-run.

Changes to tests and the harness:

- `KeySerializerTests.fs`: the serializer's output is asserted verbatim as the documented version 1 leaderboard token, every key case round-trips through `Serialize` and `Deserialize`, and `KeySerializer.next` returns a version 1 token. The tests of the legacy writer's output and its failure on a missing table key were removed with the writer. The legacy decoding and rejection tests are unchanged. The earlier note that the documented "legacy writer's output" is asserted (tasks 1.2–1.3) no longer applies; the legacy decode example still is.
- `RolloutCompatibilityTest.fs` now covers the single release against a test copy of the original serializer: the released serializer reads its own version 1 tokens in full; a legacy base-table token is accepted; a legacy index token fails with `MissingKeyAttribute "score"`; the original reader fails on a version 1 token with `KeyNotFoundException`. The reader-revision cases (byte-identical legacy output, mixed reader fleet) were removed because that revision no longer exists.
- Pool tests: `VersionOneKeySerializer` was removed. Traversal, retry, Timeline, and fan-out tests now use the production `DynamoDbKeySerializer`. Tests that used the deployed serializer to obtain legacy tokens now use `LegacyTokenKeySerializer` in `PaginationSupport.fs`, which writes legacy tokens and reads with the production serializer. The nine index rejections, legacy layout-match traversal, the fan-out failure with a `pk`/`sk`-only writer (`MissingKeyAttribute "getGamblersByPoolLayoutPk"`), and joined-gambler materialization with legacy tokens are all kept.
- PoolLayout tests: the private `VersionOneKeySerializer` was removed in favor of `DynamoDbKeySerializer`. The legacy-token tests already used literal legacy JSON.
- Verification harness (`Pagination.fs`): `TypedWriterKeySerializer` was replaced by `DynamoDbKeySerializer`. The legacy checks use a `LegacyTokenKeySerializer` instead of the production serializer.

`docs/backend-pagination.md`:

- "Rollout" now describes one release: the deployment targets, how earlier tokens are handled, and rollback (never to the original serializer).
- A new "Future token-format changes" subsection records the readers-first, writer-later rule for when Fortuna has live traffic.
- The query start-key note, the service-fixture description, the coverage list, and the fan-out replay step no longer refer to a writer revision.

Commands, run from `Amazon.Lambda/` with .NET SDK 10.0.201:

| Command | Result |
|---|---|
| `dotnet build Felipearpa.Tyche.slnx` | Succeeded, 0 errors, 0 warnings; builds both HTTP hosts (`Felipearpa.Tyche.HttpApi`, `Felipearpa.Tyche.AmazonLambda`) and the verification harness |
| `dotnet test Felipearpa.Data.DynamoDb/test --no-build` | Passed: 104, failed: 0 (99 before: 4 legacy-writer tests removed, 1 writer test and 10 serializer round trips added, rollout combinations reduced from 6 to 4) |
| `dotnet test Felipearpa.Tyche.Pool/test --no-build` | Passed: 253, failed: 0 |
| `dotnet test Felipearpa.Tyche.PoolLayout/test --no-build` | Passed: 13, failed: 0 |
| `dotnet test Felipearpa.Tyche.AmazonLambda/test --no-build` | Passed: 113, failed: 0 |
| `dotnet test Felipearpa.Tyche.HttpApi/test --no-build` | Passed: 27, failed: 0 |
| `dotnet test Felipearpa.Tyche.Function/test --no-build` | Passed: 1, failed: 0 |

Not verified:

- The `pagination` service fixture against DynamoDB with the production serializer as writer. This task did not use AWS. The fixture in task 3.2 already traversed every operation with version 1 tokens from an equivalent test writer.
- The deployed release, the absence of new invalid-key errors after it, and a real fan-out replay.

Accepted without the AWS `pagination` harness run using the production serializer as writer, a real deployment, and a real fan-out replay by the user on 2026-10-11.

## iOS — task 4.2 (2026-10-11)

Status: not done. The bump to `lazy-paging-swift` 0.0.4 is in place, resolves through `Tyche.xcworkspace`, and builds. On the simulator, 0.0.4 stopped the request storm from task 4.1, but it did not stop the duplicate appends: the client still sends up to 4 identical requests per token, and lists still stall with blank space or missing rows. The release changes only the refresh started when a list appears. Its `append()` still checks `isLoading` before it awaits `pageFetcher.canAppend()`, as in 0.0.3.

### Package bump

- `exact: "0.0.4"` is pinned in `iOS/UI/Package.swift`, `iOS/Pool/Package.swift`, and `iOS/Bet/Package.swift`. No `Package.swift` in the repository still pins 0.0.3.
- `iOS/Tyche.xcworkspace/xcshareddata/swiftpm/Package.resolved` differs from `HEAD` only in the `lazy-paging-swift` entry: revision `498585e69eba9a2ef2f24d27c1933928e7cba86d`, version 0.0.4, which matches the remote tag `v0.0.4`. `git diff` showed the same 4 files and 5 changed lines before and after `xcodebuild`. No Firebase or Google entry changed.
- The per-package `Package.resolved` files in `iOS/UI`, `iOS/Bet`, `iOS/Pool`, and `iOS/Account` still record 0.0.3 (`119f42b`). The workspace build does not use them, and they were left unchanged.
- The 0.0.4 source diff from 0.0.3 adds `LazyPagingItems.refreshOnAppear()`, which skips the appearance refresh when its task is already cancelled, and makes `LazyPagingVStack` call it. Nothing else in `Sources` changed.

### Environment

- Tables: `Pool-verify-20261011` and `PoolLayout-verify-20261011` in account 567776770335, `us-east-2`, created again with `AWS_REGION=us-east-2 ./pool-table.sh create-copy Pool-verify-20261011 PoolLayout-verify-20261011` and deleted at the end. No production `Pool` or `PoolLayout` item was read or written, and nothing was deployed.
- Host: the scratch HttpApi host from task 4.1, outside the repository. It now uses the production `IKeySerializer` registration (`DynamoDbKeySerializer`, the version 1 writer since task 3.4) instead of the removed switchable serializer. It still forces `Limit` on the eight affected indexes, here `PAGE_LIMIT=4` and `RANKED_PAGE_SIZE=4`. The run used a fresh request log with the same fields as task 4.1.
- Data: the same synthetic design as task 4.1 for the account used there. It has a 15-member main pool, 11 other pools, 36 bets for the account (14 settled, 7 live, 15 upcoming), 10 open layouts, and 1 closed layout. The seed now also gives member "dani" 34 more bets.
- App: scheme `Tyche Local`, built through `Tyche.xcworkspace` with `xcodebuild -resolvePackageDependencies`, then `xcodebuild ... -destination 'id=A2F5EF6F-BA25-4035-9494-F6ACE0144047' 'URL_BASE_PATH=http:/$()/localhost:5098' build`. The build succeeded, and the built `Info.plist` has `URL_BASE_PATH` set to `http://localhost:5098`. The app ran on the iPhone 16 Pro (iOS 18.1) simulator, which was still signed in. The run was read-only: navigation, scrolling, and one pull-to-refresh.

### Results

The host logged 99 requests, all 200. 76 carried `next`, and each matched a token the host had issued earlier. A burst means requests for the same token that arrived within 2 seconds.

| List (screen) | Token shape | Requests per token | Rows shown vs expected |
|---|---|---|---|
| My pools | `v1{pk:S,score:N,sk:S}` | 4 for every token, over 3 traversals (6 bursts, 24 requests) | 8 of 12, then blank space. After a pull-to-refresh: 4 of 12, with no append |
| Leaderboard (Scores) | `v1{pk:S,score:N,sk:S}` | 4, 4 | 12 of 15 (down to karen), then blank space. The last page was never requested |
| Pending (Bets) | `v1{matchDateTime:S,pk:S,sk:S}` | 1 for 8 tokens, 2 for 1 token | Reached the last bet (26/10). The bet on 4/10 came back twice and showed as a row without a date header, followed by blank space |
| History | `v1{computedDateTime:S,pk:S,sk:S}` | 4, 4 (plus 1 after a reload) | 8 of 14 (down to 29 Sep), then blank space. The last page was never requested |
| Timeline (dani) | `live:v1{matchDateTime:S,pk:S,sk:S}`, then `finished:v1{computedDateTime:S,pk:S,sk:S}` | 1 for 7 tokens, 2 for 1 `live:` token | Live entries, then finished entries down to 20 Sep. The client carried `finished:` tokens to the last page |
| Members (Gamblers, owner) | `v1{gamblerUsername:S,pk:S,sk:S}` | 4, 4 | Stopped around julio of 15, then blank space |
| Open layouts ("Pool from layout") | `v1{pk:S,sk:S,startDateTime:S,status:S}` | 4, 4 | 8 of 10, the same as task 4.1 |
| Ranked match bets (Score) | Ranked cursor | Single page | Both participants shown |

Duplicate appends happen when several rows appear at once, such as when a list first renders or when a fling exposes several rows. Lists scrolled one row at a time, like pending bets and Timeline, sent one request per token. These are the task 4.1 symptoms, except that no leaderboard row repeated in this run; the leaderboard stalled instead.

Request storm: none. After the Bets tab was opened and the match Score screen was pushed over the pool home, the log stayed unchanged for more than 20 seconds. Then the Gamblers screen was pushed, and the log stayed unchanged for another 15 seconds. Going back to the pool home reloaded History once. In task 4.1, the same steps caused about 110 first-page requests per second. 0.0.4 fixes the storm.

### Not observed

- A run at `PAGE_LIMIT=10`. The 4-item run already showed the failure, so a second limit was not run.
- A rejected legacy token, because the host no longer writes one. Recovery was covered in task 4.1.
- Presentation conditions (dark mode, large text, other widths): the presentation is unchanged, so they were not checked.

Cleanup: the app was terminated and the host was stopped. Both verify tables were deleted, and `aws dynamodb list-tables --region us-east-2` lists only Account, MatchScoreIngestion, Pool, and PoolLayout. The simulator was shut down. `git status` shows only the existing iOS bump, untracked change files, and this note outside `Amazon.Lambda/`.

## iOS — task 4.2 rerun on 0.0.5 (2026-10-11)

Status: done. With `lazy-paging-swift` 0.0.5, every affected list sent one request per token and showed every expected row exactly once. A pull-to-refresh during an in-flight append did not add the stale page, and the request storm did not return. One limitation from task 4.1 remains: after a pull-to-refresh, a first page whose rows all fit on screen does not append. It does not depend on the library version; see "Client limitation".

### Package bump

- `exact: "0.0.5"` is pinned in `iOS/UI/Package.swift`, `iOS/Pool/Package.swift`, and `iOS/Bet/Package.swift`, and no `Package.swift` pins another version.
- The workspace `Package.resolved` differs from `HEAD` only in the `lazy-paging-swift` entry: revision `70ad048dd98d63cb372528970c5879bd56b47d9a`, version 0.0.5, which matches the remote tag `v0.0.5`.
- The file's checksum was the same before and after `xcodebuild -resolvePackageDependencies` and the build. `git diff` showed the same 4 files and 5 changed lines, and no Firebase or Google entry changed.
- The checked-out package was at `70ad048`.
- The per-package `Package.resolved` files in `iOS/UI`, `iOS/Bet`, `iOS/Pool`, and `iOS/Account` still record 0.0.3. The workspace build does not use them, and they were left unchanged.

### Environment

The setup was the same as the first 4.2 run:

- Tables: `Pool-verify-20261011` and `PoolLayout-verify-20261011` in us-east-2, created with `pool-table.sh create-copy` and deleted at the end. No production `Pool` or `PoolLayout` item was read or written.
- Host: the scratch HttpApi host with the production serializer, `PAGE_LIMIT=4` and `RANKED_PAGE_SIZE=4`, logging to a new file.
- Data: the same synthetic data for the same account, seeded with 136 Pool items and 47 PoolLayout items.
- App: `Tyche Local`, built through `Tyche.xcworkspace` with the `URL_BASE_PATH=http://localhost:5098` override. The build succeeded. The app ran on simulator A2F5EF6F, which was still signed in. The run was read-only.

For the refresh check, the host was restarted with a scratch-only setting that delays every request carrying `next` by 6 seconds.

While restarting the host, I also stopped the app by mistake: the command that ended the host process matched every process connected to port 5098, including the app, which received SIGTERM. The app was relaunched, signed in, and the run continued.

### Results

The host logged 62 requests, all 200. 37 carried `next`, and each matched a token the host had issued earlier. Requests are grouped by token when they arrive within 2 seconds of each other. Every token produced exactly one request:

| List (screen) | Token shape | Requests per token | Rows shown vs expected |
|---|---|---|---|
| My pools | `v1{pk:S,score:N,sk:S}` | 1 (6 tokens over 3 traversals) | 12 of 12, down to "Paginación 12", with no gap |
| Leaderboard (Scores) | `v1{pk:S,score:N,sk:S}` | 1 (4 tokens) | 15 of 15 (pages of 4, 4, 4, 3), down to nico and marta, who are tied at 0 |
| Pending (Bets) | `v1{matchDateTime:S,pk:S,sk:S}` | 1 (9 tokens) | Pages of 1, 1, 2, 1, 1, 4, 4, 4, 4, then an empty final page: 22 of 22, from 22/09 to 26/10. The 4/10 row showed once, with its date header |
| History | `v1{computedDateTime:S,pk:S,sk:S}` | 1 (5 tokens over 2 traversals) | 14 of 14 (pages of 4, 4, 4, 2), down to 20 Sep |
| Timeline (dani) | `live:v1{matchDateTime:S,pk:S,sk:S}`, then `finished:v1{computedDateTime:S,pk:S,sk:S}` | 1 (8 tokens) | Live pages of 2, 1, 1, 2, 1, then finished pages of 4, 4, 4, 2: 7 live and 14 finished entries, down to 20 Sep |
| Members (Gamblers, owner) | `v1{gamblerUsername:S,pk:S,sk:S}` | 1 (3 tokens) | 15 of 15 (pages of 4, 4, 4, 3), alphabetical, down to verificacion |
| Open layouts ("Pool from layout") | `v1{pk:S,sk:S,startDateTime:S,status:S}` | 1 (2 tokens) | 10 of 10 (pages of 4, 4, 2), down to "Plantilla verificación 10" |
| Ranked match bets (Score) | Ranked cursor | Single page | 2 participants on the match that was opened |

None of the first 4.2 run's symptoms appeared: no duplicate requests, no row without a date header, no blank space before the end of a list, and no list that stopped before its last page.

Refresh during an append, on the leaderboard with the 6-second delay:

1. The first page (4 rows) loaded, and the client sent one append with the page-1 token, which the host held for 6 seconds.
2. A pull-to-refresh during that delay sent a first page without `next` (4 rows, 108 ms).
3. The held append then returned 4 rows. The list kept only the 4 refreshed rows (ana, carla, bea, dani), and no stale rows were added. With 0.0.3, the stale page would have been added to the refreshed list.

Request storm: none. With the match Score screen pushed over the pool home after the Bets tab was opened, the log did not grow for 20 seconds. Returning to the pool home reloaded History once.

### Client limitation

These predate 0.0.5 and do not depend on the token format.

- After a pull-to-refresh, if the refreshed first page fits on screen, the list does not append until a new row appears. This was already noted in task 4.1. It happened on My pools at normal speed (4 of 12 after the refresh). It also happened on the leaderboard after the discarded append (4 of 15, with no further request).
- In the leaderboard case, 0.0.5 discarded the stale page but did not request the next page again, because no row appeared. Scrolling or navigating away and back loads the rest.

### Not observed

- A run at `PAGE_LIMIT=10`. The 4-item run crosses more page boundaries.
- A prepend: the iOS lists do not prepend.
- A rejected legacy token, covered in task 4.1.
- Presentation conditions (dark mode, large text, other widths): the presentation is unchanged, so they were not checked.

Cleanup: the app was terminated and the host was stopped. Both verify tables were deleted, and `aws dynamodb list-tables --region us-east-2` lists only Account, MatchScoreIngestion, Pool, and PoolLayout. The simulator was shut down. `git status` shows only the 0.0.5 bump, the untracked change files, and this note outside `Amazon.Lambda/`.

Accepted without a page-size-10 run, prepends, a rejected legacy token on 0.0.5, and presentation conditions by the user on 2026-10-11.
