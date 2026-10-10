# Pool settlement operations

Runbook for the ordered settlement flow, rejoin restoration, the pool-total projection, and the ranked match-bets rollout. How the flow works is described in [docs/pool-settlement.md](../../docs/pool-settlement.md); the records are in [docs/database-model.md](../../docs/database-model.md).

All scripts default to the production stack (`TycheApi`, tables `Pool` and `PoolLayout`, region `us-east-2`). Set `POOL_TABLE_NAME`, `POOL_LAYOUT_TABLE_NAME`, `POOL_COMMAND_QUEUE_URL`, and `POOL_MAINTENANCE_QUEUE_URL` to target other resources.

## Resources

| Resource | Purpose |
|---|---|
| `UpdatePositionsQueue.fifo` | Ordered pool commands (group = pool). Visibility 1800 s, retention 14 days, redrive after 30 receives. |
| `UpdatePositionsQueue-dlq.fifo` | Pool commands that exhausted their receives. The pool keeps its active fence. |
| `PoolMaintenanceQueue` / `PoolMaintenanceQueue-dlq` | Propagation, migration, and reconciliation jobs; redrive after 5 receives. `OnPoolMaintenanceJob` runs at most 16 concurrent invocations (`PoolMaintenanceMaximumConcurrency`). |
| `tyche-stream-failures-567776770335` | Stream batches that exhausted 10 retries or 6 hours, with their complete records; expires after 90 days. |
| `tyche-pool-operations-alarms` (SNS) | Target of every alarm below. It has no subscriptions; subscribe an email or chat integration to be notified. |
| `GetPoolMatchBetsByTotal-index` | Ranking index on `Pool`, owned by `pool-table.sh`. |

The `Pool`, `PoolLayout`, and `MatchScoreIngestion` tables are not CloudFormation resources. `pool-table.sh` is the reviewed owner of what this repository adds to `Pool`:

```bash
./pool-table.sh describe Pool                 # keys, indexes, stream, billing
./pool-table.sh ensure-ranking-index Pool     # idempotent, additive
```

Stream ARNs are stack parameters (`PoolStreamArn`, `PoolLayoutStreamArn`, `MatchScoreIngestionStreamArn`); update them if a stream is ever re-created.

## Alarms and metrics

Custom metrics are in the `Tyche/PoolOperations` namespace, dimension `Handler`. Each record also logs `poolId`, `operationId`, and `messageId` for searching in CloudWatch Logs.

| Alarm | Fires when | First step |
|---|---|---|
| `PoolCommandDeadLettersAlarm` | A command is in the FIFO dead-letter queue | [Dead-lettered command](#dead-lettered-command) |
| `PoolMaintenanceDeadLettersAlarm` | A maintenance job exhausted its retries | [Dead-lettered job](#dead-lettered-job) |
| `PoolBlockedAlarm` (`OperationBlocked`) | An operation failed 5 times without progress | [Blocked pool](#blocked-pool) |
| `ActiveOperationAgeAlarm` | An operation has been active for over 30 minutes | Check `OperationYielded` vs `CommandFailures` for the pool |
| `PoolCommandBacklogAgeAlarm` | A command waited over 15 minutes | Is the consumer enabled (`SettlementProcessingEnabled`)? Is a pool blocked? |
| `ProjectionBacklogAgeAlarm` | A maintenance job waited over 15 minutes | Propagation lag: ranked order is behind totals |
| `StreamRecordFailuresAlarm`, `Pool…IteratorAgeAlarm` | Stream records keep failing, or handling is over 10 minutes behind | [Retained stream failure](#retained-stream-failure) |

Other metrics: `OperationCompleted`, `OperationYielded` (deliberate deadline yields, not failures), `CommandFailures`, `CommandDeferred`, `CommandAlreadyCompleted`, `SettledDetails`, `UnresolvedDetails`, `ResultCommandsDispatched`, `TotalsPropagated`, `ProjectionsInitialized`, `ReadinessUnresolved`, `Reconciled*` (including `ReconciledRestorations`, restorations requested by reconciliation), `RestoreHeldDetails` (retained details held for repair, counted by reconciliation), `JobFailures`. A restoration reports its rebound details as `SettledDetails` and its held details as `UnresolvedDetails`.

## Procedures

### Correct a published result

```bash
./correct-result.sh <poolLayoutId> <matchId> <home> <away> <currentResultVersion>
```

The script refuses, without writing anything, if the match's current version is not the one given or if any pool has not completed settlement of that version. On success it publishes the next result version; each pool then applies the difference between the old and new award to the total, advances the score version, and recalculates positions, in order, with the current formula. A repeated correction is a no-op; an older version can never be reapplied. This replaces the former manual procedure (direct detail and total writes, hand-built position messages, hardcoded point values).

### Dead-lettered command

1. Read the message: `aws sqs receive-message --queue-url <dlq> --attribute-names All --message-attribute-names All` (do not delete it).
2. Find the cause in the `OnUpdatePositions` logs by `messageId` or `operationId`.
3. If the pool root still has an `activeOperation` for it, the pool waits for that operation; later commands help finish it. After fixing the cause, move the message back:
   `aws sqs start-message-move-task --source-arn <dlq-arn> --destination-arn <queue-arn>`.
4. A command rejected because its operation ID was reused with another payload never took the fence; investigate the producer and delete the message.

### Blocked pool

```bash
./unblock-pool.sh <poolId>             # inspect the checkpoint
# fix the cause (data repair, permissions, throughput)
./unblock-pool.sh <poolId> --unblock
```

Then redrive the dead-lettered command (above) or send any command for the pool. Commands received while it was blocked are stored as `DEFERRED` receipts and are enqueued again, in order, when the blocked operation completes.

### Dead-lettered job

Maintenance jobs are version guarded and safe to repeat. Fix the cause and move them back with `start-message-move-task`, or enqueue `RECONCILE_POOL` for the pool.

### Retained stream failure

Objects in the failure bucket (`aws/lambda/<mapping-uuid>/<shard>/<date>/…`) hold the complete batch of a failed invocation: `requestContext.condition` says why (for example `RetryAttemptsExhausted`), and `payload` is a JSON string whose `Records` carry the keys and both images. Read the records with `aws s3 cp <object> - | jq -r '.payload | fromjson | .Records[] | .eventName, .dynamodb.Keys'`, replay as below, and delete the object once the replay is verified.

- PoolLayout records with a new result: `./request-match-reconciliation.sh <matchId>`.
- PoolLayout match insertions: `./enqueue-maintenance.sh RECONCILE_POOL <poolId>` for the layout's pools (materializes missing details and requests settlement).
- Pool membership insertions or total changes: `./enqueue-maintenance.sh RECONCILE_POOL <poolId>`. For an insertion, also replay its restoration with `./restore-rejoin-history.sh <poolId> <gamblerId> --replay`: reconciliation requests a restoration only for details bound to an earlier generation, and a detail retained from before generations existed has none.
- Pool membership removals: `./enqueue-maintenance.sh RETIRE_MEMBERSHIP <poolId> <gamblerId> <membershipGeneration from OldImage>`, then `RECONCILE_POOL`.

### Rejoin restoration

Every join enqueues `RESTORE_REJOIN_HISTORY` (`RESTORE#<gamblerId>#<membershipGeneration>`), which runs as an ordered pool operation; the flow is in [docs/pool-settlement.md](../../docs/pool-settlement.md#rejoin-restoration). Its failures use the procedures above: a command that keeps failing is retried by the queue, then blocks the pool after 5 claims without progress ([Blocked pool](#blocked-pool)) or reaches the dead-letter queue ([Dead-lettered command](#dead-lettered-command)); a join whose stream record could not be handled is in the failure bucket ([Retained stream failure](#retained-stream-failure)). Rebinding is guarded per detail, so replaying a restoration never counts an award twice.

Inspect a gambler's restoration (read-only): the membership, the restore receipt (`operationState`, `unresolvedDetails`), and every computed detail not bound to the membership, with the reason for any held one:

```bash
./restore-rejoin-history.sh <poolId> <gamblerId>
```

A detail listed without `heldFor` waits for restoration (the receipt is missing or the operation is still running); one whose `generation` is the earlier membership's and `heldFor` is the current generation was held for repair.

### Held rejoin detail

A restoration holds a retained detail for repair, outside the total, when it is malformed (`computed detail without an award`), its match is missing or has no result, or it reflects a newer result version than its match (`detail reflects a newer result than its match`). The rest of the history is restored.

1. Inspect it: `./restore-rejoin-history.sh <poolId> <gamblerId>`, then read the full detail and its layout match:

   ```bash
   aws dynamodb get-item --region us-east-2 --table-name Pool --consistent-read \
     --key '{"pk":{"S":"GAMBLER#<gamblerId>#POOL#<poolId>"},"sk":{"S":"MATCH#<matchId>"}}'
   aws dynamodb get-item --region us-east-2 --table-name PoolLayout --consistent-read \
     --key '{"pk":{"S":"POOLLAYOUT#<poolLayoutId>"},"sk":{"S":"MATCH#<matchId>"}}'
   ```

2. Repair the data that caused the hold (for example restore the detail's award from its prediction and the match result, or restore the layout match). Do not change the detail's `poolMembershipGeneration`, and do not change the membership total by hand.
3. Clear the marker of each repaired detail, conditioned on it being held for the current membership: `./restore-rejoin-history.sh <poolId> <gamblerId> --clear <matchId>`.
4. Replay the restoration: `./restore-rejoin-history.sh <poolId> <gamblerId> --replay`. It enqueues the command under a new operation ID (`RESTORE#<gamblerId>#<membershipGeneration>#OPERATOR-<time>`), because the first restoration's receipt is already completed. Details restored earlier are skipped; the repaired ones are rebound and their awards added once, and positions are recalculated.
5. Confirm with the inspect command that no computed detail is left unbound. The replay's receipt is `SETTLEMENT#<operation ID printed by --replay>` on the pool partition; it is `COMPLETED` with `unresolvedDetails` 0 when nothing was held again.

### Migration and readiness

Per pool, in order, waiting for the maintenance queue to drain between steps:

```bash
./enqueue-maintenance.sh INITIALIZE_IDENTITIES --all
./enqueue-maintenance.sh BACKFILL_PROJECTION --all
./enqueue-maintenance.sh VERIFY_READINESS --all
```

Readiness is stored on each pool root (`rankingReadiness`). A `NOT_READY` pool with `unresolved > 0` usually has a membership held by identity initialization (its computed awards do not add up to its total); the `IdentityMembersHeld` metric record lists it. Repair the data, then run the three steps again for that pool.

### Rollout and rollback

| Step | How |
|---|---|
| Quiesce settlement | Deploy with `SettlementProcessingEnabled=false`; stream records (24 h) and queued commands (14 days) are retained. |
| Activate settlement | Deploy with `SettlementProcessingEnabled=true` after identities are initialized. |
| Ranked reads | Available for every pool whose `rankingReadiness` is `READY`. Ranked requests to any other pool answer `503 RANKING_NOT_READY`. |
| Roll back ranked reads | Either the mobile apps stop sending `sort=pool-score-desc`, or mark the affected pools not ready with `./mark-pool-not-ready.sh <poolId> [reason]`. Requests without `sort` are unaffected. |
| Re-enable ranked reads for a pool | `./enqueue-maintenance.sh VERIFY_READINESS <poolId>`; the pool answers ranked requests again only if the check records `READY`. |
| Roll back settlement | Keep the versioned writers, receipts, and checkpoints. Pause with `SettlementProcessingEnabled=false`, fix, re-enable, then run `RECONCILE_POOL` for affected pools. Never redeploy the previous unguarded scoring alongside active operations, never delete the original indexes, and never rewrite totals by hand. |

Deploy parameters are passed as `dotnet lambda deploy-serverless --template-parameters "SettlementProcessingEnabled=false"`.

### Capacity

The account's Lambda concurrency limit is 1,000, shared by every function. Settlement (`OnUpdatePositions`) is not capped; it runs at most one invocation per pool with queued commands. Propagation and backfill are capped at 16 concurrent invocations, which propagated one result across a 5,000-member pool in about 210 s. Raise or lower that cap with `--template-parameters "SettlementProcessingEnabled=true;PoolMaintenanceMaximumConcurrency=<n>"` (2 to 1,000). A higher cap finishes propagation sooner, at the cost of more concurrent writes to `Pool` and more of the shared quota. If a function's CloudWatch `Throttles` metric rises, check `aws lambda get-account-settings` before raising any cap.
