# Verification

## Backend — tasks 1.1–1.27 (2026-10-08)

Work ran from 2026-10-08 into 2026-10-09 (UTC). Captures are in [verification/](verification/).

### Schema and ownership (1.1, 1.14)

- `docs/database-model.md` was read before any persistence change and now documents the added records: pool root ordering state (`membershipRevision`, `operationRevision`, fixed-size `activeOperation` checkpoint, `lastCompletedOperationRevision`, `rankingReadiness`), settlement receipts (`POOL#<poolId>` / `SETTLEMENT#<operationId>`), membership generation and score version, detail identity, result version, position markers, the total projection, the ranking key encoding, and the access patterns.
- Key separation: receipts carry no `score`, `gamblerUsername`, `matchDateTime`, or `computedDateTime`, so they stay out of every existing index; membership reads use `begins_with(sk, "GAMBLER#")`; `DeletePool` now also deletes `SETTLEMENT#` items. Checked by the unit test “a duplicate command is a no-op after completion and keeps its receipt” (asserts no `score` on the receipt) and by the production pool scan below.
- Legacy computed-state mapping: a computed detail without `resultVersion` reflects result version 1; a layout result without `resultVersion` is version 1 (`SettlementPolicyTest`, `PoolLayoutEventFilterTest`).
- Ownership: the Pool/PoolLayout/MatchScoreIngestion tables stay outside CloudFormation (importing risks replacement). `Amazon.Lambda/operations/pool-table.sh` is the reviewed owner of the additive schema (`describe`, `ensure-ranking-index`, `create-copy`) with `operations/ranking-index.json`; stream ARNs became stack parameters.
- Production Pool table before the change: [backend-1.1-pool-table-before.json](verification/backend-1.1-pool-table-before.json). Added `GetPoolMatchBetsByTotal-index` (hash `getPoolMatchBetsByTotalPk`, range `getPoolMatchBetsByTotalSk`, INCLUDE projection of 20 attributes). The first attempt with 22 projected attributes was rejected by DynamoDB (limit 20), so `poolScoreVersion` and `poolMembershipGeneration` are not projected; the read path does not need them. Existing indexes and keys unchanged; status ACTIVE.

### Stream, queue, and failure protections (1.2)

Deployed to stack `TycheApi` before activating the new settlement (first deploy with `SettlementProcessingEnabled=false`). Rendered mappings after deploy: [backend-1.2-event-source-mappings-after.json](verification/backend-1.2-event-source-mappings-after.json) (before: [backend-1.2-event-source-mappings-before.json](verification/backend-1.2-event-source-mappings-before.json), which had unlimited retries, no bisection, and no failure destination).

| Mapping | Retries | Record age | Bisect | Partial batch | On failure |
|---|---|---|---|---|---|
| `OnPoolChange` (Pool stream) | 10 | 21600 s | yes | yes | S3 `tyche-stream-failures-567776770335` |
| `OnMembershipChange` (Pool stream, filter `POOL#`/`GAMBLER#`) | 10 | 21600 s | yes | yes | S3 |
| `OnPoolLayoutChange` | 10 | 21600 s | yes | yes | S3 |
| `OnMatchScoreIngestionChange` | 10 | 21600 s | yes | no (handler returns no partial response) | S3 |
| `OnUpdatePositions` (FIFO, batch 1) | queue redrive 30 → `UpdatePositionsQueue-dlq.fifo` | | | yes | |
| `OnPoolMaintenanceJob` (batch 5, max concurrency 2) | queue redrive 5 → `PoolMaintenanceQueue-dlq` | | | yes | |

Queues: `UpdatePositionsQueue.fifo` visibility 1800 s, retention 14 days; DLQs 14 days; `PoolMaintenanceQueue` visibility 720 s. Nine CloudWatch alarms publish to SNS topic `tyche-pool-operations-alarms` (no subscriptions). Budgets are documented in `docs/pool-settlement.md`.

Recoverability beyond stream expiry was observed in production: four rehearsal records that kept failing (a malformed rehearsal match ID, see 1.24) were written to the bucket with `requestContext.condition = RetryAttemptsExhausted`, `approximateInvokeCount = 11`, and `payload` holding the complete records with keys and images; they were replayed with the documented procedure and then deleted. The maintenance dead-letter alarm fired for eight malformed operator messages (an operator-script quoting bug, fixed; see Issues found) and returned to OK after they were purged. Active-fence retention after DLQ transfer: unit test “a later command helps finish an operation stuck after its message exhausted retries” and the isolated-resource check “failed commands reach the dead-letter queue without holding the fence”.

### Failure-path tests and baseline (1.3)

The AGENTS.md exploration prerequisite is about ARTEMIS mobile exploration and does not apply to backend F# tests.

A stateful fake DynamoDB client (`Felipearpa.Tyche.Pool/test/Fakes`) parses and applies the condition, key, filter, projection, and update grammar the repositories use; applies conditional writes and `TransactWriteItems` atomically with per-item cancellation reasons; rejects unused expression attribute names and values (as DynamoDB does); serves GSI and eventually consistent reads from a frozen snapshot on demand; injects faults before or after commit; withholds `BatchGetItem` keys; and records a stream-like change log. `FakeFifoQueue` models per-group order, deduplication, receive counts, redrive, and send failures.

Baseline against the pre-refactor code (fixture at that time: A 5, B 10, C 8; capture [backend-1.3-baseline-legacy.txt](verification/backend-1.3-baseline-legacy.txt)):

| Scenario | Intended outcome | Baseline |
|---|---|---|
| Lost post-commit notification | Retry rediscovers computed pools and finalizes positions | Failed: positions never updated |
| Cancellation is not settlement | Retryable cancellation leaves the bet pending for retry | Failed: bet left uncomputed, pool reported settled |
| Partial position write | Previous positions survive the retry | Failed: `beforePosition` overwritten |
| Older operation replay | Replay of A after B changes nothing | Failed: match 1 previous positions rewritten |
| Lagging leaderboard index | Positions from authoritative totals | Failed: stale order persisted |
| Match inserted with its result | Every detail settled | Failed: details stayed uncomputed |
| Insertion and result in one delivery | Converge in either order | Failed: details stayed uncomputed |
| Interrupted settlement | Resumes, awards applied once | Passed |

After the refactor all eight pass (`SettlementFailurePathTest`); the fixture gained a computed historical match so identity initialization can bind it (A 4, B 10, C 6), and every injected fault is asserted to have fired. The pre-refactor harness was removed with the code it exercised.

### Implementation summary (1.4–1.23)

- 1.4: `ComputeBets`, `UpdatePositions`, `IUpdatePositionsPublisher`, and the repository `Compute`/`UpdatePositions` loops are removed. Orchestration lives in application services (`DispatchMatchResult`, `ProcessPoolCommand`, passes, materializers, lifecycle, migration, reconciliation, `GetRankedPoolMatchBets`) and Lambda handlers; repositories only read and perform single guarded writes; messaging is in publishers. `BetEvaluator` is unchanged and `BetEvaluatorTest` passes unchanged.
- 1.5–1.12, 1.15–1.23: as described in `docs/pool-settlement.md`. Corrections go through `CorrectMatchResultRequestBuilder` and `operations/correct-result.sh`; the hardcoded 5/2/2/1 point values of the old runbook are gone (the current formula is 10/4/4/2 in `BetEvaluator`).
- 1.13: `RankingKey` with a ten-digit complement over the whole Int32 domain (`RankingKeyTest`).
- Ranked endpoint: `GET /pools/{poolId}/matches/{matchId}/bets?sort=pool-score-desc`; status codes in `docs/pool-settlement.md`.

### Automated checks (1.27)

`dotnet test Felipearpa.Tyche.slnx` (from `Amazon.Lambda`): 316 passed, 0 failed (Pool 145, AmazonLambda 83, Account 19, Type 54, PoolLayout 7, DynamoDb 4, Core 2, Function 1, Crypto 1). Before the change: 210.

Coverage by task, all on the fake client unless noted: 1.6 duplicate admission, payload mismatch, A/B/A replay with a new join, deadline yields, expired workers, assistance by a later command, blocked pool deferral and repair, atomic completion, fixed root attributes (`PoolOperationTest`); 1.7 continuation through empty pages, partial enqueue retry including settled pools (`DispatchAndStandingsTest`); 1.8 zero awards, missing and removed memberships (`PoolOperationTest`); 1.9 lagging leaderboard index, join during the pass (`SettlementFailurePathTest`, `PoolOperationTest`); 1.10 standings before any result, removal after a settled match (`DispatchAndStandingsTest`); 1.11 late join, join racing a result, insertion and result under index lag (`CorrectionAndMaterializationTest`); 1.12 corrections, duplicate and older replays, prerequisite refusal, interrupted correction; 1.15–1.18 propagation to historical matches without touching predictions/results/awards, unchanged totals, duplicates and reversals, newer decreases, retried failures, continuations past 500 rows, removal tombstones, rejoin fencing, legacy removal events (`ProjectionTest`); 1.17 stale binding at creation (`CorrectionAndMaterializationTest`); 1.5/1.19/1.20 identity binding, held memberships, restartable backfill, overlapping total change, readiness under membership change, reconciliation of missing details, unsettled matches, lagging copies, stale positions and generations, contention retries (`MigrationAndReconciliationTest`); 1.21–1.23 order by totals not awards, no-bet leader, all-zero ties across pages exactly once, unavailable tail, one index query and no enrichment per page, cursor context and malformed cursors, non-member and removed requesters, hidden before lock and after a reschedule, computed match revealed, readiness and switch, unknown pool or match, refresh after convergence (`RankedMatchBetsTest`); routing, auth, disabled mode, and index outage → 503 (`GetMatchBetsTest`); message codecs and cursors (`PoolMessageJsonTest`, `RankedMatchBetsCursorTest`); stream filters (`PoolLayoutEventFilterTest`, `MembershipEventFilterTest`).

### Integration on isolated resources (1.27)

Harness: `Amazon.Lambda/Felipearpa.Tyche.Verification` runs the production handler classes against real DynamoDB tables and streams and real SQS queues. Temporary resources (all deleted): tables `Pool-verify-20261008`, `PoolLayout-verify-20261008` (created with `pool-table.sh create-copy`); queues `PoolCommands-verify-20261008.fifo` (+ `-dlq.fifo`), `PoolMaintenance-verify-20261008` (+ `-dlq`); bucket `tyche-verification-567776770335-20261009`; IAM role and instance profile `TycheVerification-20261009`; EC2 instance `i-0a3c4b4509bbdf544` (c7g.xlarge, us-east-2). The runs moved to the instance because the workstation sleeps while unattended, which left SDK requests signed and held for over 15 minutes (`Signature expired`).

Result (run `integration-20261009T072050Z`): 18 of 18 checks passed — [backend-1.27-integration-checks.txt](verification/backend-1.27-integration-checks.txt), [json](verification/backend-1.27-integration-checks.json). Highlights: migration through the real maintenance queue reached READY and a held membership kept its pool NOT_READY; deadline yields were redelivered by SQS with receive counts 1, 2, 3; positions came from authoritative totals; the ranked page converged 13.2 s after the result write (including two deliberate yields); FIFO order kept a standings reconciliation (revision 2) ahead of the next result (revision 3); a reused operation ID with another payload went to the dead-letter queue after 3 receives without holding the fence; a legacy `poolId|matchId` message was processed as a match reconciliation; a correction applied the award difference once and reordered the feed; a removed requester was denied before cleanup and left the feed after it; a rejoin got a new generation without rebinding the retained detail; a match inserted with its result was settled for every member. No stream batch needed a retry in that run.

An earlier run found a real defect: back-to-back transactions on one membership are cancelled with `TransactionConflict` in DynamoDB, and identity binding and backfill treated that as done. Fixed with bounded contention retries, per-member sequential backfill writes, and a regression test that cancels every other transaction.

### Load (1.25)

Run `load-20261009T080937Z` and `latency-20261009T094559Z` on the same instance: [load](verification/backend-1.25-load-measurements.json), [latency](verification/backend-1.25-latency-measurements.json). Pool of 5,000 members plus a 50-member pool on the same layout, 100 matches each (510,052 seeded items), all totals starting at zero.

| Measurement | Result |
|---|---|
| Seeding | 510,052 items in 174 s; table write throttle events 138,749 in that window (retried by the SDK) |
| Settlement + positions, one result, 5,000 members | 30.3 s in one invocation (receive count 1); the 50-member pool 0.2 s |
| Two results back to back in the same pools | 20.2 s and 25.8 s, in order |
| Correction of a settled result | 15.8 s |
| Replay of a completed operation | under 0.1 s, no writes |
| Checkpoint yields (3-second invocations) | 7 yields, max receive count 8 (budget 30), completed |
| Projection propagation after one result | pipeline idle after 210 s with 16 local maintenance workers; 15,860 maintenance deliveries, 0 failed; table write throttle events 1,449 and 601 in the following 5-minute windows, ranking index 1,076 in total, all absorbed by retries |
| Ranked first page, single viewer | p50 8 ms, p95 12 ms (3 access-check reads + 1 index query) |
| Concurrent viewers (one client host) | 10: p50 10 / p95 32 ms; 50: 39 / 76 ms; 200: 274 / 445 ms; 1,000-viewer burst of 1,400 page reads: p50 935–1,219 ms, p95 1,497–1,788 ms, 0 failures, no read throttling |
| Position pass reads | 5,000 memberships in 50 strongly consistent pages, 0.38 s; sort 9.4 ms; 5,000 detail strong batch reads 1.42 s |
| Consumed capacity (whole run) | table 2.18 M WCU, 0.47 M RCU; ranking index 3.2 M WCU |

Limits and caveats, stated rather than fixed: latency figures measure the data path from one in-region client; they exclude API Gateway and Lambda, and the growth with concurrency reflects that single client as much as DynamoDB. The account's Lambda concurrency limit is 10 in total, so the deployed endpoint cannot serve 1,000 concurrent viewers until that quota is raised. Propagation was measured with 16 workers; the deployed maintenance mapping allows 2 (because of the same quota), so propagation for a 5,000-member result is expected to take several times longer — not measured. No service budget was violated for settlement (300 s timeout, 30 receives).

### Production rollout and rehearsal (1.20, 1.24)

1. Added the ranking index (above). Deployed with settlement quiesced (`OnPoolLayoutChange` and `OnUpdatePositions` disabled; the queue was empty, so no legacy position messages needed draining).
2. `INITIALIZE_IDENTITIES` for all 58 pools: 0 job failures, no held memberships. Scan afterwards: 110 of 110 memberships have a generation; 11,037 of 11,037 member details are bound; 266 details of removed gamblers stay unbound by design.
3. Deployed with `SettlementProcessingEnabled=true` (no unguarded legacy scorer exists in the new code).
4. `BACKFILL_PROJECTION` and `VERIFY_READINESS` for all pools: 58 of 58 READY (three pools were first reported NOT_READY because the readiness jobs were enqueued before the backfill queue had really drained; re-verified READY).
5. Rehearsal with a temporary layout `01M4GBWTBG0X3MQVNG54AD0RVX`, pool `01M4GBWTBGQV8TFDA5TT0FZ0QG`, three members, and a test account (all deleted afterwards, including the four retained failure objects): joins produced standings operations; a malformed match ID in my rehearsal data made the membership and layout records exhaust retries into the failure bucket, and `RECONCILE_POOL` replayed them; the result settled through the deployed mappings in about 3 s with propagation to the other match; a legacy message was processed (revision 5); the ranked endpoint (invoked directly with JWT claims) answered 503 while disabled, then, after enabling, 200 in pool-total order; `correct-result.sh` refused an unmet prerequisite version and then applied 1-0 → 0-0 (A 20 → 4, B 0 → 12) with the feed reordered.
6. Rollback rehearsal: with `RankedMatchBetsEnabled=false` and `SettlementProcessingEnabled=false`, the ranked request answered 503, the unsorted request kept answering 200, and a result written while quiesced stayed unsettled for 45 s; after re-enabling, it settled 39 s after the stack update. Versioned writers, receipts, and checkpoints stayed in place throughout.

Final production state: both switches `true`, all queues and dead-letter queues empty, the failure bucket empty, alarms OK. Toggles used `aws cloudformation update-stack --use-previous-template`; code deploys used `dotnet lambda deploy-serverless` from `Felipearpa.Tyche.AmazonLambda/src`.

### Issues found and fixed during verification

- FIFO deduplication is queue-wide, and every pool's result command shares its operation ID, so only one pool would have received a result; deduplication IDs now include the pool (caught by the fake queue).
- Deferred commands re-enqueued within the deduplication window would have been dropped; they are now republished with fresh deduplication IDs.
- DynamoDB transaction contention on one membership (see Integration).
- Layout kickoff times were parsed into local time, which can break the reveal gate off UTC hosts; now parsed as UTC.
- `enqueue-maintenance.sh` split JSON bodies under some shells; bodies are built as variables and sent with `SendMessageBatch`.
- Exceptions were not written by the Lambda logger; pool handlers now log them.

### Not verified

- Lambda event-source behavior on isolated resources (batching, bisection, partial responses) — the harness invoked the handlers directly; these were exercised only on the deployed mappings during the production rehearsal.
- Propagation time with the deployed maintenance concurrency of 2, and endpoint throughput through API Gateway and Lambda under the 10-execution account limit.
- The HTTP endpoint through API Gateway with a real Firebase token (no credentials available); the function was invoked directly with the JWT claim shape.
- `HttpApi` local host routing for the ranked mode: built, not run.
- Alarm notifications: the topic has no subscription.

#### Follow-up: ranked switch removed (2026-10-10)

The global ranked switch is gone; ranked reads depend only on each pool's `rankingReadiness`. Removed: stack parameter `RankedMatchBetsEnabled`, environment variable `RANKED_MATCH_BETS_ENABLED`, `RankedMatchBetsSettings.Enabled` and its check, the HttpApi `RankedMatchBets:Enabled` setting, the harness option `RankedEnabled`, and the disabled-mode test cases. A pool that is not READY still answers `503 RANKING_NOT_READY` (`RankedMatchBetsTest` “an unready pool fails recoverably…”, and `GetMatchBetsTest` “a pool that is not ready fails recoverably without querying any index”). `SettlementProcessingEnabled` is unchanged. New operator command for rollback: `Amazon.Lambda/operations/mark-pool-not-ready.sh <poolId> [reason]`; re-enable with `./enqueue-maintenance.sh VERIFY_READINESS <poolId>`.

| Check | Command | Result |
|---|---|---|
| Backend suite | `dotnet test Felipearpa.Tyche.slnx` (from `Amazon.Lambda`) | 316 passed, 0 failed |
| Deploy | `dotnet lambda deploy-serverless --template-parameters "SettlementProcessingEnabled=true"` (from `Felipearpa.Tyche.AmazonLambda/src`) | `UPDATE_COMPLETE` at 2026-10-10T13:26:25Z; stack parameters are now `PoolStreamArn`, `PoolLayoutStreamArn`, `MatchScoreIngestionStreamArn`, `SettlementProcessingEnabled=true`; `GetMatchBets` has no environment variables |
| Ranked request, pool `01KTPAWF326C43QE3VDK8FC2CS`, a computed match, direct function invoke with the pool owner's email claim | `aws lambda invoke` with `sort=pool-score-desc` | 200, 13 rows |
| Mark not ready | `./mark-pool-not-ready.sh 01KTPAWF326C43QE3VDK8FC2CS "follow-up rollback rehearsal"` | ranked request 503 `RANKING_NOT_READY`; the same request without `sort` 200 with 13 rows |
| Re-verify | `./enqueue-maintenance.sh VERIFY_READINESS 01KTPAWF326C43QE3VDK8FC2CS` | `READY`, unresolved 0, 1,352 details checked; ranked request 200, 13 rows in the same order as before |

Final production state: 58 of 58 pools READY, all six event-source mappings enabled, all queues and dead-letter queues empty, failure bucket empty, nine alarms OK. No production data was added; the only write was the readiness record of the rehearsal pool, restored to READY by the re-verification.

Not verified: ranked requests through API Gateway with a real Firebase token (the function was invoked directly with the claim shape); the HttpApi local host was built but not run.

Accepted without alarm notification delivery (1.2, 1.19) by the user on 2026-10-10. The alarm topic stays without subscribers for now; push notifications will be added later in a separate task.

Alarm email delivery verified on 2026-10-10, superseding the acceptance above: felipearcila@gmail.com subscribed to `tyche-pool-operations-alarms` and confirmed; `aws cloudwatch set-alarm-state` forced `TycheApi-PoolBlockedAlarm` to ALARM, the history shows "Successfully executed action" on the topic, the user received the email, and the alarm returned to OK.

Accepted without starting the local HttpApi host (1.21–1.23) by the user on 2026-10-10. The real-token gap is covered by the iOS and Android on-device runs, whose ranked requests went through API Gateway with a real Firebase token and answered 200.

Accepted without Lambda batching and batch-splitting checks on the isolated resources (1.27) by the user on 2026-10-10; these were exercised only by the deployed mappings during the production rehearsal.

#### Follow-up: quota 1,000 and 1.25 completion (2026-10-10)

The account's Lambda concurrency limit is now 1,000 (`aws lambda get-account-settings`: `ConcurrentExecutions` 1000, `UnreservedConcurrentExecutions` 1000).

Maintenance concurrency: the template now takes `PoolMaintenanceMaximumConcurrency` (default 16, the worker count of the propagation measurement above) for `OnPoolMaintenanceJob`'s `ScalingConfig.MaximumConcurrency`. `OnUpdatePositions` stays uncapped.

| Check | Command | Result |
|---|---|---|
| Template | `aws cloudformation validate-template --template-body file://serverless.template.yaml` | valid; parameters include `PoolMaintenanceMaximumConcurrency` |
| Backend suite | `dotnet test Felipearpa.Tyche.slnx` (from `Amazon.Lambda`) | 316 passed, 0 failed |
| Deploy | `dotnet lambda deploy-serverless --template-parameters "SettlementProcessingEnabled=true"` (from `Felipearpa.Tyche.AmazonLambda/src`) | not run: the session's permission check refused the production deploy; the deployed mapping still shows `MaximumConcurrency` 2 |

Endpoint burst: two bursts of 1,000 concurrent synchronous direct invocations of `TycheApi-GetMatchBets-ymAItrpAQHBn` with `sort=pool-score-desc`, pool `01KTPAWF326C43QE3VDK8FC2CS` (READY, 13 members), computed match `01KQ3HPW41N7XVP1B1QACTGRHG`, and the email claim of the user's own account. The client was one laptop (Python threads released by a barrier, SigV4-signed `Invoke` calls, `LogType=Tail` for each `REPORT` line). Read-only; summaries in [endpoint burst](verification/backend-1.25-endpoint-burst.json).

| Measurement | Cold burst (17:23:40Z) | Warm burst (17:23:56Z) |
|---|---|---|
| Responses | 1,000 × 200, 13 rows each, one identical order | 1,000 × 200, 13 rows each, the same order |
| Function errors / non-200 | 0 / 0 | 0 / 0 |
| Lambda duration p50 / p95 / max | 851 / 895 / 965 ms (cold requests 855 / 896 / 965 ms; the 136 warm requests 22 / 51 / 56 ms) | 26 / 50 / 86 ms |
| Cold starts (Init Duration present) | 864; init p50 275 ms, p95 300 ms, max 611 ms | 0 |
| Client round trip p50 / p95 / max | 4,758 / 5,944 / 6,581 ms | 4,225 / 5,029 / 7,912 ms |
| Concurrent executions (CloudWatch, 1-minute max) | 878 | 148 |

CloudWatch for the function over 17:20–17:30Z: `Throttles` 0, `Errors` 0, `Invocations` 1,995 (2,006 were sent, including a 5-invocation smoke run and one single check; the remainder had not yet been counted when read). The account-wide `Throttles` was also 0.

Caveats: the client round trip is dominated by the single laptop host (1,000 simultaneous TLS connections from one process over the internet), not by the function. Lambda durations show the service-side cost. That host also did not hold all 1,000 requests in flight at once: concurrency peaked at 878 instances. In the warm burst, instances served several requests each, so concurrency peaked at 148. API Gateway was not under test: no real Firebase tokens, so the function was invoked directly with the claim shape.

Final production state: 58 of 58 pools READY; all six event-source mappings enabled (`OnPoolMaintenanceJob` at `MaximumConcurrency` 2 until the deploy runs, the others uncapped); all queues and dead-letter queues empty; the failure bucket empty; nine alarms OK. The bursts wrote nothing.

Deploy completed later the same day, superseding the "not run" deploy row above: the user ran `dotnet lambda deploy-serverless --template-parameters "SettlementProcessingEnabled=true"` from `Amazon.Lambda/Felipearpa.Tyche.AmazonLambda/src`. Stack `TycheApi` reached `UPDATE_COMPLETE` at 2026-10-10T17:36:49Z with parameters `PoolMaintenanceMaximumConcurrency=16` and `SettlementProcessingEnabled=true`. `aws lambda list-event-source-mappings --region us-east-2` shows `OnPoolMaintenanceJob` enabled with `MaximumConcurrency` 16; the other five mappings are enabled and uncapped.

Accepted without production propagation timing at the deployed concurrency of 16, API Gateway under the burst, and a full 1,000-in-flight peak (1.25) by the user on 2026-10-10. Covered by the 16-worker load measurement (210 s), the two 1,000-invocation direct bursts (peak 878 concurrent, 0 throttles, 0 errors), and the earlier 1,000-viewer index burst.

#### Follow-up: contended fence writes and live-lease yields (2026-10-10)

Review finding: `GuardedWrite.succeeded` turned transaction contention on the pool root into `false`, so a contended completion or checkpoint save was reported as `Yielded` without releasing the lease. The handler made the message visible at once, the next delivery met the live lease and yielded again, and each lap consumed one receive, so a single transient conflict could reach the redrive limit inside the five-minute lease and dead-letter a healthy operation. The harness hid it by raising on every yield and moving the clock past the lease on every exception.

Changes: `AdmitAsync`, `ReplaceCheckpointAsync`, and `CompleteAsync` return `GuardedWriteOutcome`; `ProcessPoolCommand` retries `Contended` on those writes with `GuardedWriteRetry.backoff` (now shared with the settlement pass) and treats only `ConditionNotMet` as lost ownership; a claim that meets a live lease, or a fence write that finds another owner, reports `RetryAfter` (the rest of that lease), which `PoolOperationEvent` passes to `ChangeMessageVisibility` instead of 0; `VersionedSettlementHarness` advances its clock by `RetryAfter` on yields and past the lease only on failures. `docs/pool-settlement.md` describes the new behaviour.

| Check | Command | Result |
|---|---|---|
| New tests | `PoolOperationTest` "a contended completion is tried again and completes on the same delivery" and "a delivery that meets a live lease waits for that lease instead of claiming"; `PoolOperationEventTest` "a command that meets a live lease stays invisible for the rest of that lease" | pass |
| Mutation check | the old behaviour restored temporarily (contention collapsed to `false`, live-lease yield without `RetryAfter`) | all three fail: outcomes `[Yielded; OperationCompleted]`, no `RetryAfter`, visibility 0; the fix was then restored byte-identical |
| Backend suite | `dotnet test Felipearpa.Tyche.slnx` (from `Amazon.Lambda`) | 319 passed, 0 failed |

Not deployed: the change is in the working tree; `dotnet lambda deploy-serverless` was not run.

#### Follow-up: added-match fan-out identity (2026-10-10)

Review finding: `FanOutMatchToGamblers` requested settlement for a match that already had a result with `RESULT#<matchId>#<version>`, the identity result dispatch uses. When that operation had already completed for the pool (a result processed before the match rows existed, such as an insert record replayed from the failure bucket after its result was handled), the completed receipt answered `AlreadyCompleted` and the new details stayed uncomputed until an operator ran `RECONCILE_POOL`, against design decision 4 ("a repair after genuine late materialization has a new identity … does not reuse a completed command as its only wake-up").

Changes: the fan-out takes the inserting stream event's id as its source and publishes `PoolCommand.reconcileMatch` (`MATCH#<matchId>#<eventId>`), as the join materializer already does; `PoolLayoutEvent` passes `record.EventID`; the harness passes `INSERT#<matchId>`. The commands table in `docs/pool-settlement.md` moves the fan-out to the `RECONCILE_MATCH` row.

| Check | Command | Result |
|---|---|---|
| Tests | `FanOutMatchToGamblersTest` "given an added match that already has a result when fanned out then a reconciliation named after the insert is requested" (updated) and "given a result already settled for the pool when its match is fanned out late then the new details are still settled" (new) | pass |
| Mutation check | the settlement identity restored temporarily | both fail: the published id is `RESULT#…`, and the late details stay uncomputed; the fix was then restored byte-identical |
| Backend suite | `dotnet test Felipearpa.Tyche.slnx` (from `Amazon.Lambda`) | 320 passed, 0 failed |

Not deployed: the change is in the working tree; `dotnet lambda deploy-serverless` was not run.

#### Follow-up: standings reconciliation no longer restarts the leaderboard trend (2026-10-10)

Review finding: every join or removal runs a positions-only operation, every member counted as that operation's first write, and `PositionPolicy.decide` recorded `beforePosition := position` for all of them. The leaderboard arrows (`beforePosition - position`) therefore went blank for everyone whenever a member joined or left, while `beforeScore` kept meaning the total before the latest settled match. Before this change positions were written only at scoring. Decided with the user on 2026-10-10: the trend compares with the last scored match.

Changes: `PositionChange.MembershipBeforePosition` is optional and `PositionPolicy.decide` takes the operation target; a match settlement or correction records the previous position on its first write, a standings reconciliation moves `position` and its markers and leaves `beforePosition` untouched (a new member has none until a match is scored); the membership position builder assigns `beforePosition` only when the change carries one. `docs/pool-settlement.md` (positions stage) and `docs/database-model.md` (membership attributes) describe the rule.

| Check | Command | Result |
|---|---|---|
| Tests | `PositionPolicyTest` "a standings reconciliation moves the position without restarting the trend" (new) and the settlement cases now naming the target; `DispatchAndStandingsTest` "a removal after a settled match…" now expects the untouched member to keep its previous position and the displaced one to keep the position match1 left; "a join before any result…" expects no previous position for the new member | pass |
| Mutation check | standings reconciliation recording the baseline again, temporarily | the policy test and the removal test fail (the untouched member's previous position reset to 1); the fix was then restored byte-identical |
| Backend suite | `dotnet test Felipearpa.Tyche.slnx` (from `Amazon.Lambda`) | 321 passed, 0 failed |

Not deployed: the change is in the working tree; `dotnet lambda deploy-serverless` was not run. Existing memberships keep whatever `beforePosition` the last operation wrote; the next scored match sets it under the new rule.

#### Follow-up: settlement transformer moved to Infrastructure (2026-10-10)

Review finding: `PoolSettlementDictionaryTransformer` sat in the Domain folder but encoded and decoded the receipt's command with `PoolMessageJson`, the SQS wire codec, so `Infrastructure\PoolMessageJson.fs` was compiled in the middle of the Domain block of `Felipearpa.Tyche.Pool.fsproj`.

Changes: the transformer now lives in `Infrastructure/` under the `Felipearpa.Tyche.Pool.Infrastructure` namespace (it maps DynamoDB attribute dictionaries and embeds the queue's wire format, and its only consumers are the settlement repository and the two request builders); `PoolMessageJson.fs` is back in the Infrastructure block, followed by the transformer, before `PoolSettlementRequestBuilder.fs`; the three consumers open the new namespace. No behaviour changed. The sibling transformers stay in Domain: they predate this change and reference table constants only.

| Check | Command | Result |
|---|---|---|
| Compile order | `grep 'Compile Include' Felipearpa.Tyche.Pool.fsproj` | after `Infrastructure\PoolTable.fs` (pre-existing) the Domain block contains Domain files only, up to `Infrastructure\GuardedWrite.fs` |
| Backend suite | `dotnet test Felipearpa.Tyche.slnx` (from `Amazon.Lambda`) | 321 passed, 0 failed |

Not deployed: the change is in the working tree; `dotnet lambda deploy-serverless` was not run.

## iOS — tasks 2.1–2.9 (2026-10-10)

Devices: simulator `iPhone 16 Pro (iOS 18.1)` (A2F5EF6F-BA25-4035-9494-F6ACE0144047) with the production `Tyche` scheme (`https://tyche-api.felipearpa.com`), and the `medium_phone` emulator (`emulator-5554`, Android 16, API 36) for the ARTEMIS exploration. Both were already signed in to the user's account. All checks were read-only: no bets, pools, or accounts were changed. Captures are in [verification/](verification/) with the prefix `ios-`.

### Exploration of the existing flow (2.1)

- Android, ARTEMIS Pro trace `7b12c1b1-5133-4295-af3d-9624e76ed66a` (verification level `final`, 5 of 5 checks passed), on the existing app installed with `./gradlew :app:installProdDebug`: pool "Copa Mundial de la FIFA 2026" (13 members, viewer 8th with 589 points). Entry path: History tab → finished match card → "Score". Header: flags, names, `1 - 2`, `7/15/26, 2:00 PM`. All 13 rows on one page, viewer row first and not highlighted, missing predictions left blank (no "No bet"), awards `+0`/`+4`/`+20`. Other row → that gambler's Timeline; Back returns to Score; own row does nothing; Home returns to the pool screen; pull to refresh shows the spinner. Every match of this pool has finished: the Bets tab shows "Nothing to show" and no Timeline row is upcoming, so the upcoming-entry and hidden states could not be explored. Locators: top bar `Back`/`Home` content descriptions at `[12,85][138,211]` and `[944,85][1070,211]`; title text `Score`; tabs `Scores`/`Bets`/`History`; History cards expose one content description (`Sunday, Jul 19, 2:00 PM. Final score: España 0, Argentina 0. Your bet: 3 to 2. 0 points`); leaderboard rows `Rank 1, El mono, 676 points, Rank unchanged`, the viewer row `clickable=false`. Seen in passing (not in scope): the Android History tab showed "Couldn't load your points." with Retry.
- iOS, existing app built from the unchanged tree: same entry path; header `0 - 0` with `19/07/26, 2:00 PM` beside the teams; rows in the legacy order (viewer first: felipearpa, Santiago Arcila, Gracias guerreros, Juano, El mono, …) with blank predictions and `+0` awards ([ios-2.1-baseline-computed.png](verification/ios-2.1-baseline-computed.png)); the leaderboard at the same time ranked El mono 676, Val Cardo 657, Juano 654, Santiago Arcila 616, juandahoyos28 607, Gracias guerreros 604, Abuela 592, felipearpa 589 ([ios-2.1-baseline-leaderboard.png](verification/ios-2.1-baseline-leaderboard.png)). Tapping the viewer row did nothing (the router dropped it); tapping Santiago Arcila opened his Timeline. In code, rows used a tap gesture without button semantics and had no combined VoiceOver label, and `invokeMatchOpen` ignored entries whose betting was open.

### Implementation (2.2–2.8)

- 2.2 `DataBet`: `GetRankedPoolMatchGamblerBetsUseCase` and `PoolGamblerBetRepository.getRankedPoolMatchGamblerBets` replace the unsorted match feed (its only caller was this screen). Requests send `sort=pool-score-desc` and `next` only when there is a cursor (the previous request sent the literal `next=nil` on first pages, because Alamofire encodes an optional `nil` inside `[String: Any]` as `nil`). Error bodies map to `RankedMatchBetsError`: 409 `PREDICTIONS_HIDDEN` → `predictionsHidden`, 503 `RANKING_NOT_READY`/`RANKING_UNAVAILABLE` → `rankingUnavailable`, 400 `INVALID_CURSOR`/`CURSOR_CONTEXT_MISMATCH` → `cursorRejected`; 401/403/404 keep `NetworkError`. `RankedMatchBetsFeed` keeps server order, deduplicates by gambler ID within a generation, rejects pages of an older generation, refills a page emptied by deduplication up to three times, restarts automatically once after a rejected continuation (again only after a successful append or a viewer refresh), and reports the hidden answer to the view model. Refresh waits for the page in flight, then reloads from the first page and replaces the rows.
- 2.3 `MatchBetListView` takes `viewerAccountId` (the router passes the signed-in account) and is keyed by account, pool, and match. The heading/gate comes from the routed gambler's bet; the own prediction comes from that same response when the route is the viewer, otherwise from the viewer's own single-bet request (404 means no bet), with its own retry. Heading, own-bet, and feed requests carry generations. The participant list is created only after the gate is confirmed; a hidden answer from the feed or a heading that reopens betting replaces the pager, so earlier rows and their accessibility nodes are gone.
- 2.4 `MatchHeader`: status line (Starts pill / Bets closed pill / date and time), teams around VS, a match score, or a prominent final score with dashes when unavailable; stacked at accessibility sizes. Date and time reuse `HistoryMatchDateFormat`. `MatchHiddenBetsContent`: neutral lock, "Bets are hidden", explanation, own prediction (placeholder / Your bet / No bet / failure with retry), centered in the space below the heading and scrolling when it does not fit.
- 2.5 `assets/icons/home.svg` and `assets/icons/lock.svg` (Material Symbols outlined, weight 400, optical size 24, repository commit `bd8cb85b…`, SHA-256 `41735f05…a913` and `148a3da5…315c`); their paths equal the geometry both platforms already used. iOS image sets are byte-identical copies with template rendering and preserved vector data (`lock_FILL0_wght400_GRAD0_opsz24.svg` renamed to `lock.svg`). `assets/icons/README.md` records sources and the Android derivation for task 3.4. Back stays the native navigation button (OS-rendered, excluded); Home keeps its localized "Home" label.
- 2.6 `MatchGamblerBetItem`: shared `AccountAvatar` (leaderboard fallback; current-user colors on the viewer's fallback), wrapping name with a break opportunity after `@`, `You` marker and `currentUserContainer` row, prediction or "No bet", and once computed a `HistoryPointsPill` (extracted from `HistoryBetItem`, which keeps its look) with a reserved width so pills line up; at accessibility sizes the prediction and pill move under the name and the pill under the prediction when needed.
- 2.7 Other rows are `Button`s with `InteractiveRowButtonStyle`; the viewer row and placeholders have no action. Loading uses the production header and row with placeholder models and `isPlaceholder` (empty avatar ID). Empty, full-list error (scoped retry restarts from the first page), append error (retry repeats the page, or restarts after a rejected continuation), and refresh failure ("Couldn't update the match. Showing the last details." with retry) are wired. `invokeMatchOpen` now opens upcoming entries (Timeline rows); pending rows of the Bets tab keep their editing controls and still have no row tap.
- 2.8 New English/Spanish/Spanish (Spain) strings, all `extractionState=manual`; removed the unused `live_label`, `predictions_open_title`, and `predictions_open_message`. One announcement each for the heading, a participant (name, You, team-qualified prediction or No bet, award with plural units), the hidden explanation, and the own prediction; flags, avatars, lock, and separators are hidden.

Measured contrast (WCAG, composited): viewer-row neutral pill text 4.65:1 light / 6.89:1 dark; viewer-row positive pill 6.56 / 8.03; viewer-row text 5.52 / 9.50; status pill text 13.32 / 11.38; secondary text (VS, lock, No bet) 5.95 / 7.72.

### Checks (2.9)

| Check | Command | Result |
|---|---|---|
| App build | `xcodebuild -workspace Tyche.xcworkspace -scheme Tyche -configuration Debug -destination 'platform=iOS Simulator,id=A2F5EF6F-…' build` | Succeeded |
| DataBet and Bet tests | `xcodebuild test … -scheme Tyche -only-testing:DataBetTests -only-testing:BetTests CODE_SIGNING_ALLOWED=NO` | DataBet 9 passed; Bet 85 passed (new: `RankedMatchBetsRequestTests`, `RankedMatchBetsFeedTests`, `MatchBetListViewModelTests`, `MatchBetsPresentationTests`; `MatchBetListViewModelReloadTests` replaced) |
| Package regression (History, Timeline, avatar, placeholder, UI paging, Pool ViewInspector) | same with `-only-testing:UITests -only-testing:PoolTests -only-testing:AccountTests` | all passed (32 + 93 and the XCTest cases) on iOS 18.1 |
| App-hosted tests | `xcodebuild test … -only-testing:TycheTests` (signing on) | Succeeded. With `CODE_SIGNING_ALLOWED=NO` the host app crashed before connecting on this machine. |

On the simulator, signed in, production data, computed match España 0–0 Argentina:

- Pool-total order across the feed: El mono, Val Cardo, Juano, Santiago Arcila, juandahoyos28, Gracias guerreros, Abuela, felipearpa (You, highlighted), C.A.Vélez, jdiazu03, Alf28, Juan Pablo Rojas, giraldocano517 — the leaderboard order above, while awards are +4 only for Juano and Santiago Arcila, so order does not follow awards ([light](verification/ios-2.9-computed-light.png), [dark](verification/ios-2.9-computed-dark.png), [bottom](verification/ios-2.9-computed-dark-bottom.png)). All 13 arrived in one page (page size 50).
- Viewer row: no action. Other row: opens Timeline; from Timeline the same match opens with "Timeline" as Back and the viewer still marked You ([capture](verification/ios-2.9-from-timeline.png)). Home returns to the pool.
- Pull to refresh kept the rows and reloaded them.
- Spanish (es-CO) ([capture](verification/ios-2.9-computed-es.png)) and the largest accessibility size ([capture](verification/ios-2.9-computed-ax5.png)); the first AX5 run overflowed the row horizontally, fixed by letting the award move below the prediction.
- States that real data cannot stage were rendered from the production components with preview fixtures by a throwaway `ImageRenderer` test (deleted afterwards): hidden with a bet, no bet, failed and loading own prediction, bets closed with and without a score, computed with an unavailable result, loading placeholders, 320-pt width, 874 × 402 landscape, 820-pt iPad width in dark, and accessibility size 5 (`ios-2.9-render-*.png`).

Reference comparison: the hidden and bets-closed renders follow the hidden and revealed references (centered start pill, flags and names around VS, lock group centered below, Your bet line; avatars, names, predictions, explicit No bet, thin separators); "In progress" is "Bets closed" and the hidden copy says "when betting closes", as the design requires. The computed screen follows the finished reference with the date above a prominent final score; pills read "+4 pts"/"0 pts" (the reference shows bare numbers) and are wider so they line up.

Simulator settings changed and restored: appearance (light → dark → light), content size (large → accessibility-extra-extra-extra-large → large), app language (launched once with `-AppleLanguages (es)`; normal launch afterwards). `iOS/Bet/Package.resolved` gained a ViewInspector pin on every build and was reverted each time.

### Not verified (iOS)

- Hidden state, bets-closed state, own-prediction states, upcoming-entry activation from Timeline, the server's 409 path, rescheduled reopening, ranked 503 and rejected cursors, multi-page ranking and ties across pages, a no-bet or zero-award leader, an unavailable award, and refresh after projection convergence: every match of the user's pools has finished and the account is read-only, so none could be observed on a device. Covered by `MatchBetListViewModelTests` (gate, own bet from route or viewer, 404 as No bet, failure and retry, stale heading, hidden answer drops rows, reopening drops rows, refresh replaces the feed, rejected continuation restarts once then offers retry), `RankedMatchBetsFeedTests` (order, deduplication, generations, refills, 409, 503), `MatchBetsPresentationTests` (state mapping, header center, announcements, `invokeMatchOpen` for upcoming entries), `RankedMatchBetsRequestTests`, and the renders.
- VoiceOver and keyboard traversal on a device, Reduce Motion, canceled press on a row, and the absence of placeholder avatar requests on the wire: checked through the accessibility-label and `avatarAccountId` tests and the shared components, not with VoiceOver, a keyboard, or CFNetwork logging.
- Real landscape rotation and an iPad: the iPad simulator is not signed in and rotation could not be driven from the tools available; covered only by the 874 × 402 and 820-pt renders.
- The number of requests after recovery on the wire: covered by the request-order assertions in `MatchBetListViewModelTests`.

Accepted without on-device observation of the items listed under "Not verified (iOS)" (2.1–2.4, 2.6–2.9) by the user on 2026-10-10. They are covered by MatchBetListViewModelTests, RankedMatchBetsFeedTests, MatchBetsPresentationTests, RankedMatchBetsRequestTests, and the component renders listed above.

## Android — tasks 3.1–3.8 (2026-10-10)

Devices: `medium_phone` emulator (`emulator-5554`, Android 16, API 36, gesture navigation, en-US, signed in to the user's account, production backend) and the user's Samsung SM-G955F (`ce0417143a6ab8130c`, Android 9, API 28, es-CO, three-button navigation, signed in). Instrumented tests ran only on `Pixel_7_API_36_no_account` (signed out), one emulator at a time. All device checks were read-only: no bets, pools, or accounts were changed. Both signed-in devices received the prodDebug build with `adb install -r` (versionCode 12, the version already installed) and stayed signed in. Captures are in [verification/](verification/) with the prefix `android-`.

### Exploration of the existing flow (3.1)

`adb devices -l` listed `emulator-5554` (`sdk_gphone64_arm64`, `medium_phone`) and `ce0417143a6ab8130c` (`SM_G955F`). ARTEMIS Pro trace `ea364b8f-bc1b-4562-b5ad-c4c7c9d7d4b9` on `medium_phone` with the build installed before this work (stopped during its final review after every plan step was checked; the notes are complete):

- Pools: "Copa Mundial de la FIFA 2026" (13 members), "Copa Mundial de la FIFA prur" (1 member), "Prueba" (3 members).
- The observations of iOS task 2.1 still applied: History card → Score; legacy order with the viewer first; missing predictions blank; viewer row does nothing; other row → that gambler's Timeline (no pending Timeline entries in the 2026 pool); Timeline entry → Score; Back returns to Timeline and then Score; Home returns to the pool screen (History tab); pull to refresh shows the Material spinner.
- New: the Back and Home buttons of the old Score screen had no content description (`NAF="true"`), and every participant row, the viewer's included, was `clickable=true`.
- New: "Copa Mundial de la FIFA prur" has a locked, uncomputed match (Colombia vs Portugal, 10/2/26 8:00 PM) on its Bets tab, shown with a padlock and no score fields; its card is clickable. This is the only bets-closed match reachable with real data. No pool has a match whose betting is open, so the hidden state cannot be reached on a device.
- Locators: top bar Back `[12,85][138,211]` and Home `[944,85][1070,211]` (old screen); tabs `Scores`/`Bets`/`History` at `[0,2189][360,2337]`, `[360,2189][720,2337]`, `[720,2189][1080,2337]`; History card content description `Sunday, Jul 19, 2:00 PM. Final score: España 0, Argentina 0. Your bet: 3 to 2. 0 points`; pool-screen swap icon at about `(1006,146)`.

### Implementation (3.2–3.7)

- 3.2 `data:bet`: `GetRankedPoolMatchGamblerBets` and `PoolGamblerBetRepository.getRankedPoolMatchGamblerBets` replace the unsorted match feed (its only caller was this screen; `GetPoolMatchGamblerBets` and `GetPoolMatchBetsPagingQuery` are removed). The Ktor data source sends `sort=pool-score-desc` and `next` only with a cursor, and translates error bodies into `RankedMatchBetsException`: 409 `PREDICTIONS_HIDDEN` → `PredictionsHidden`, 503 `RANKING_NOT_READY`/`RANKING_UNAVAILABLE` → `RankingUnavailable`, 400 `INVALID_CURSOR`/`CURSOR_CONTEXT_MISMATCH` → `CursorRejected`; any other response keeps `NetworkException.Http`. `RankedMatchBetsFeed` makes one Paging source per generation (each refresh), keeps server order, drops gambler IDs already shown in the generation, refills a page emptied by deduplication up to three times, returns `LoadResult.Invalid` once after a rejected continuation (restart from the first page) and afterwards `ListChanged`, whose retry restarts the list; the automatic restart becomes available again after a successful append or a viewer restart.
- 3.3 The match destination reads the signed-in account from `CurrentAccountCoordinator` (passed from `MainActivity` as `signedInAccountId`), not from the launch snapshot or the route; the view model is keyed by account, pool, match, and routed gambler. The heading comes from the routed gambler's single bet; the own bet comes from that response when the route is the viewer, otherwise from the viewer's single-bet request (404 → No bet) with its own retry. Heading and own-bet requests carry generations. The participant pager exists only while the gate confirms bets are revealed; a 409 from the feed, or a heading that reopens betting, removes it, and a later reveal starts a new pager.
- 3.4 `MatchHeader`: Starts pill / Bets closed pill / date and time; VS, a match score, or a prominent final score with dashes; teams stack around the center at font scale ≥ 1.5 or when a word of a team name would not fit beside the center. Date and time reuse `HistoryMatchDateFormat`. `MatchHiddenBetsContent`: neutral lock, "Bets are hidden", explanation, own prediction (placeholder / Your bet / No bet / failure with retry), centered below the heading and scrolling when it does not fit. `ui/res/drawable/home.xml` and `lock.xml` are now derived from `assets/icons/home.svg` and `lock.svg` (path data copied unchanged, `translateY="960"` group, as for `add.xml`); `assets/icons/README.md` records it. The geometry did not change. The Back arrow is unchanged and not covered by a canonical source.
- 3.5 `MatchGamblerBetItem`: shared `AccountAvatar` (leaderboard fallback, current-user colors for the viewer, empty account ID and `isPlaceholder` for placeholders), wrapping name with a break opportunity after `@`, "You" and the `currentUserContainer` row, prediction or "No bet", and once computed a `HistoryPointsPill` (extracted from `HistoryBetItem`, whose look is unchanged) reserving the width of "+88 pts"; at font scale ≥ 1.5 the prediction and pill move under the name.
- 3.6 The heading is the first item of every list state (loading, loaded, empty, full-list error, centered below the heading). Other rows are one `clickable` with Material ripple; the viewer row and placeholders have no click action. Back, Home, and Timeline navigation run only while the route is started (`runIfStarted`). Pull to refresh rechecks the heading (indicator held by `isCompanionRefreshing`) and restarts the list from the first page; the hidden state has its own pull to refresh. A failed refresh keeps the heading with "Couldn't update the match. Showing the last details." and a retry. Entry points: Timeline rows already opened upcoming matches; Bets-tab pending rows keep their editing controls and no row tap, as on iOS. Top bar actions now have "Back"/"Home" names.
- 3.7 New en/es/es-ES strings; `live_label`, `predictions_open_title`, and `predictions_open_message` are removed. One announcement each for the heading (marked as a heading), a participant (name, You, team-qualified prediction or No bet, award with plural units), the hidden explanation, and the own prediction; flags, avatars, lock, and separators add no focus stops.

Contrast (WCAG, composited on the app's surfaces #FFFFFF / #121212): secondary text 5.95 light / 7.36 dark; neutral pill text 5.31 / 6.09; status pill text 12.71 / 10.77; positive pill 5.52 / 7.34; viewer-row text 5.52 / 9.50; viewer-row positive pill 6.56 / 8.03. The viewer-row neutral pill measured 4.39 light with History's neutral fill, so inside that row the neutral fill is `onCurrentUserContainer` at 12%: 4.68 light / 6.92 dark.

### Checks (3.8)

| Check | Command | Result |
|---|---|---|
| Build | `./gradlew :app:assembleProdDebug`, `:app:compileProdDebugAndroidTestKotlin` | Succeeded |
| Unit tests | `./gradlew :app:testProdDebugUnitTest :bet:testDebugUnitTest :data:bet:testDebugUnitTest :ui:testDebugUnitTest :account:testDebugUnitTest :pool:testDebugUnitTest --continue` | app 60, bet 54, data:bet 10, ui 44, account 85, pool 2; 0 failures. New: `RankedMatchBetsRequestTest` (6, Ktor MockEngine), `RankedMatchBetsFeedTest` (11), `MatchBetListViewModelTest` (12, replaced), `MatchBetsStateTest` (4) |
| Instrumented | `ANDROID_SERIAL=emulator-5554 ./gradlew :bet:connectedDebugAndroidTest :ui:connectedDebugAndroidTest :app:connectedProdDebugAndroidTest --continue` on `Pixel_7_API_36_no_account` | bet 50 with 3 failures, all the known `BetTextFieldTest` ones; ui 83 and app 26 passed. New: `MatchBetsPresentationTest` (11); `BetPlaceholderAccessibilityTest` updated |

On `medium_phone`, signed in, production data, computed match España 0–0 Argentina ([light](verification/android-3.8-computed-light.png), [end of list](verification/android-3.8-computed-light-bottom.png), [dark](verification/android-3.8-computed-dark.png), [es-CO at 200% font](verification/android-3.8-computed-es-font200-dark.png) and [its viewer row](verification/android-3.8-computed-es-font200-dark-you.png), [landscape](verification/android-3.8-computed-landscape-light.png)):

- Pool-total order: El mono, Val Cardo, Juano, Santiago Arcila, juandahoyos28, Gracias guerreros, Abuela, felipearpa (You, highlighted, `clickable=false`), C.A.Vélez, jdiazu03, Alf28, Juan Pablo Rojas, giraldocano517 — awards +4 only for Juano and Santiago Arcila. All 13 arrived in one page (page size 50). `uiautomator` content descriptions match the announcements, e.g. `El mono, Bet: España 1, Argentina 2, 0 points` and `felipearpa, You, Bet: España 3, Argentina 2, 0 points`; Back and Home are named.
- Viewer row: no action. El mono's row → his Timeline; its first entry → the same match, still marking felipearpa as You; Back → Timeline → Score; Home → pool screen.
- Pull to refresh: logcat (URLs only) shows one heading request and one ranked first page `…/bets?sort=pool-score-desc` (no `next`), both 200, through API Gateway with the app's real Firebase token.
- ARTEMIS Pro trace `87c9619a-42da-4c28-a5ab-3e15713ecb3c` (verification `final`): 9 of 9 checks passed — header without any live label, El mono first, avatars/predictions/pills, viewer row 8th and highlighted with You, viewer-row tap inert, Juano → Timeline, Back, Home, and the bets-closed header below.
- Bets closed (Copa Mundial de la FIFA prur, Colombia vs Portugal from the Bets tab): "Bets closed" pill and VS, no award pills ([capture](verification/android-3.8-closed-404-light.png)). The ranked request answered **404 `MATCH_NOT_FOUND`** while the routed single-bet request for the same pool and match answered 200 (see Issues found). The screen showed the full-list error with Retry below the heading; Retry sent exactly one request.

On the Samsung (API 28, es-CO, three-button navigation): the same match showed "Marcador", the date `domingo, 19 de jul. · 2:00 p. m.`, the same order, "Sin apuesta", "Tú", and Spanish announcements (`felipearpa, Tú, Apuesta: España 3, Argentina 2, 0 puntos`); the last row clears the transparent navigation bar ([top](verification/android-3.8-api28-es-computed.png), [end](verification/android-3.8-api28-es-computed-bottom.png)). Home returned to the pool screen. These captures predate the viewer-row neutral-pill contrast change above; the build installed at the end includes it.

States that real data cannot stage were rendered from the production components by a throwaway instrumented test on `Pixel_7_API_36_no_account` (deleted afterwards), files `android-3.8-render-*.png`: hidden with a bet (light, dark, es-CO dark, 200% font, 320 dp), no bet, failed and loading own prediction, heading placeholder, bets closed with and without a match score, computed with an unavailable result and award, list loading placeholders, ranking unavailable, and computed at 320 dp (the first 320 dp render broke "Argentina" mid-word; the teams now stack when a word does not fit).

Reference comparison: the hidden render follows the hidden reference (centered start pill, flags and names around VS, lock group centered below, Your bet line), with "when betting closes" as the design requires. Revealed rows follow the revealed reference (avatars, names, predictions, explicit No bet, thin separators) with "Bets closed" instead of "In progress". The computed screen follows the finished reference; pills read "+4 pts"/"0 pts" and share one width.

Settings changed and restored on `medium_phone`: dark mode (`cmd uimode night yes` → `no`), font scale (1.0 → 2.0 → 1.0), app locale (`cmd locale set-app-locales … es-CO` → none), rotation (`accelerometer_rotation` 1 → 0, `user_rotation` 0 → 1 → 0, `accelerometer_rotation` back to 1), TalkBack (added to `enabled_accessibility_services`, then restored to exactly `com.artemis.helper/.ArtemisAccessibilityService`, `accessibility_enabled=1`, `touch_exploration_enabled=0`). `medium_phone` was shut down for the instrumented run and relaunched with `mobile_diagnose(launch_avd="medium_phone")`; it is running, signed in, with the final build. Nothing was changed on the Samsung besides the install.

### Issues found

- Backend: for pool `01KR6QJ1NQXXP65HE719F8HTEN`, match `01M3G1W2SBVJPH9MCDFXY4CST2` (Colombia vs Portugal), `GET …/matches/{matchId}/bets?sort=pool-score-desc` answers 404 `MATCH_NOT_FOUND` while `GET …/gamblers/{gamblerId}/matches/{matchId}` answers 200 with a locked match. The ranked gate's layout read does not find a match that the pool's bet records reference. Not fixed here (backend scope).
- Existing behavior kept: list and heading failures caused by any HTTP error show the generic "Network error" text, because `orDefaultLocalized()` maps every `NetworkException` to `RemoteCommunication`.

### Not verified (Android)

- Hidden state, own-prediction states, upcoming-entry activation from Timeline, the server's 409 path, rescheduled reopening, ranked 503, rejected cursors, multi-page ranking and ties across pages, a no-bet or zero-award leader, an unavailable award, and refresh after projection convergence on a device: no match with open betting or more than one page exists in the account's pools, the only bets-closed match answers 404, and the account is read-only. Covered by `MatchBetListViewModelTest` (gate, own bet from route or viewer, 404 as No bet, failure and retry, kept own bet on failed refresh, nothing requested while hidden or unknown, 409 drops the rows after one request, reopening drops the rows, failed refresh keeps the heading, stale heading discarded), `RankedMatchBetsFeedTest` (order, deduplication, refills and their limit, generations, restart once then `ListChanged`, restart availability, 409, 503, network), `RankedMatchBetsRequestTest`, `MatchBetsStateTest`, `MatchBetsPresentationTest`, and the renders.
- TalkBack on a device: enabling TalkBack worked, but neither `adb emu event send` sequence moved accessibility focus, so no spoken order or green outline was observed. Covered by the `uiautomator` content descriptions on both devices and `MatchBetsPresentationTest` (announcements, You, no click action on the viewer row, Spanish singular point). Keyboard traversal was not checked.
- Disabled animations and the placeholder motion fallback: not toggled; the shared `LoadingPlaceholderPulse` owns it and is unchanged.
- Placeholder avatar requests on the wire: not observed in logs; `MatchBetsPresentationTest.aPlaceholderRowRequestsNoAvatarAndExposesNothing` renders a placeholder row without the avatar store, which a real avatar would need.
- Tablet / resized windows: only 320 dp renders and phone landscape on `medium_phone`.

Accepted without on-device observation of the items listed under this section's Not verified (3.1–3.8) by the user on 2026-10-10. They are covered by MatchBetListViewModelTest, RankedMatchBetsFeedTest, RankedMatchBetsRequestTest, MatchBetsStateTest, MatchBetsPresentationTest, and the renders recorded above.
