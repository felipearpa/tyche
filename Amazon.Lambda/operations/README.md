# Pool settlement operations

Runbook for the ordered settlement flow, rejoin restoration, the pool-total projection, the ranked match-bets rollout, and kickoff groups. How the flow works is described in [docs/pool-settlement.md](../../docs/pool-settlement.md); the records are in [docs/database-model.md](../../docs/database-model.md).

Every script except `bootstrap-stage.sh`, which takes the stage as its argument (`./bootstrap-stage.sh <stage>`), requires `TYCHE_STAGE`, set to `dev` or `prod`, and refuses to run without it. The stage selects the AWS account through the profile in `../stages/<stage>.sh` (`dev` for the dev account, `default` for production); the stack (`TycheApi`), tables (`Pool`, `PoolLayout`), and region (`us-east-2`) have the same names in both accounts.

```bash
TYCHE_STAGE=dev ./unblock-pool.sh <poolId>    # dev account
TYCHE_STAGE=prod ./unblock-pool.sh <poolId>   # production
```

The commands below write `TYCHE_STAGE=<stage>`; replace it with the stage you mean. Set `POOL_TABLE_NAME`, `POOL_LAYOUT_TABLE_NAME`, `POOL_COMMAND_QUEUE_URL`, and `POOL_MAINTENANCE_QUEUE_URL` to target other resources in the stage's account. The plain `aws` commands in this runbook use your default profile, which is production; add `--profile dev` to run them against dev.

## Resources

| Resource | Purpose |
|---|---|
| `UpdatePositionsQueue.fifo` | Ordered pool commands (group = pool). Visibility 1800 s, retention 14 days, redrive after 30 receives. |
| `UpdatePositionsQueue-dlq.fifo` | Pool commands that exhausted their receives. The pool keeps its active fence. |
| `PoolMaintenanceQueue` / `PoolMaintenanceQueue-dlq` | Propagation, migration, and reconciliation jobs; redrive after 5 receives. `OnPoolMaintenanceJob` runs at most 16 concurrent invocations (`PoolMaintenanceMaximumConcurrency`). |

`OnPoolMaintenanceJob` and `OnUpdatePositions` set `RecursiveLoop: Allow`. Both send work back to the queue that invokes them (job continuations, re-enqueued deferred commands). Under Lambda's default, a chain of about 16 such hops is stopped before the handler runs: nothing is logged, the message stays in flight until its visibility timeout, and it is retried until it is dead-lettered. With `Allow`, Lambda no longer emits `RecursiveInvocationsDropped` for these functions; the dead-letter alarms and backlog-age alarms still catch work that stops moving. If `RecursiveInvocationsDropped` ever appears for either function, check the deployed setting with `aws lambda get-function-recursion-config --function-name <function>`.
| `tyche-stream-failures-567776770335` | Stream batches that exhausted 10 retries or 6 hours, with their complete records; expires after 90 days. |
| `tyche-pool-operations-alarms` (SNS) | Target of every alarm below. It has no subscriptions; subscribe an email or chat integration to be notified. |
| `GetPoolMatchBetsByTotal-index` | Ranking index on `Pool`, owned by `pool-table.sh`. |
| `GetFinishedPoolGamblerBetsByMatchDate-index` | Finished details by kickoff on `Pool`, owned by `pool-table.sh`. |

The `Pool`, `PoolLayout`, and `MatchScoreIngestion` tables are not CloudFormation resources. `pool-table.sh` is the reviewed owner of what this repository adds to `Pool`:

```bash
TYCHE_STAGE=<stage> ./pool-table.sh describe Pool               # keys, indexes, stream, billing
TYCHE_STAGE=<stage> ./pool-table.sh ensure-ranking-index Pool   # idempotent, additive
TYCHE_STAGE=<stage> ./pool-table.sh ensure-kickoff-index Pool   # idempotent, additive
TYCHE_STAGE=<stage> ./pool-table.sh create-all                  # create Pool, PoolLayout, Account, MatchScoreIngestion if missing
TYCHE_STAGE=<stage> ./pool-table.sh create-copy <pool-table> <layout-table>   # isolated copies with the same schema, for verification only
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

Other metrics: `OperationCompleted`, `OperationYielded` (deliberate deadline yields, not failures), `CommandFailures`, `CommandDeferred`, `CommandAlreadyCompleted`, `SettledDetails`, `UnresolvedDetails`, `ResultCommandsDispatched`, `TotalsPropagated`, `ProjectionsInitialized`, `ReadinessUnresolved`, `Reconciled*` (including `ReconciledRestorations`, restorations requested by reconciliation), `RestoreHeldDetails` (retained details held for repair, counted by reconciliation), `KickoffCopiesMoved` (details a rescheduled match's kickoff was copied to), `KickoffCopiesBackfilled`, `JobFailures`. A restoration reports its rebound details as `SettledDetails` and its held details as `UnresolvedDetails`.

## Procedures

### Seed a pool layout

```bash
TYCHE_STAGE=<stage> ./seed-pool-layout.sh "Fortuna QA — dev" 2026-11-20T18:00:00.000Z matches.csv
```

The script uploads the CSV's matches with `Scripting/upload_matches.py` under a new layout id, then writes the `OPENED` layout root (`POOLLAYOUT#<layoutId>` as both keys, `poolLayoutVersion` 1) with the given name and start date to the stage's `PoolLayout` table, and prints the new layout id on the last line. The app lists the layout as open once the root is written. A failed run leaves unlisted match rows under the printed id; it is safe to rerun, which uses a new id.

- Name: 1 to 100 characters. Start date and match dates: UTC in the format `2026-11-20T18:00:00.000Z`.
- CSV: header `Local Team,Away Team,Date`, one match per row, as in `Scripting/example.csv`. Rows with the same team name share a team id.
- Python: it runs `Scripting/.venv/bin/python`, which needs `boto3` and `ulid-py` (`python3 -m venv Scripting/.venv && Scripting/.venv/bin/python -m pip install -r Scripting/requirements.txt`).
- It refuses `POOL_LAYOUT_TABLE_NAME`, because `upload_matches.py` always writes to `PoolLayout`.

### Correct a published result

```bash
TYCHE_STAGE=<stage> ./correct-result.sh <poolLayoutId> <matchId> <home> <away> <currentResultVersion>
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
TYCHE_STAGE=<stage> ./unblock-pool.sh <poolId>   # inspect the checkpoint
# fix the cause (data repair, permissions, throughput)
TYCHE_STAGE=<stage> ./unblock-pool.sh <poolId> --unblock
```

Then redrive the dead-lettered command (above) or send any command for the pool. Commands received while it was blocked are stored as `DEFERRED` receipts and are enqueued again, in order, when the blocked operation completes.

### Dead-lettered job

Maintenance jobs are version guarded and safe to repeat. Fix the cause and move them back with `start-message-move-task`, or enqueue `RECONCILE_POOL` for the pool.

### Retained stream failure

Objects in the failure bucket (`aws/lambda/<mapping-uuid>/<shard>/<date>/…`) hold the complete batch of a failed invocation: `requestContext.condition` says why (for example `RetryAttemptsExhausted`), and `payload` is a JSON string whose `Records` carry the keys and both images. Read the records with `aws s3 cp <object> - | jq -r '.payload | fromjson | .Records[] | .eventName, .dynamodb.Keys'`, replay as below, and delete the object once the replay is verified.

- PoolLayout records with a new result: `TYCHE_STAGE=<stage> ./request-match-reconciliation.sh <matchId>`.
- PoolLayout match insertions: `TYCHE_STAGE=<stage> ./enqueue-maintenance.sh RECONCILE_POOL <poolId>` for the layout's pools (materializes missing details and requests settlement).
- Pool membership insertions or total changes: `TYCHE_STAGE=<stage> ./enqueue-maintenance.sh RECONCILE_POOL <poolId>`. For an insertion, also replay its restoration with `TYCHE_STAGE=<stage> ./restore-rejoin-history.sh <poolId> <gamblerId> --replay`: reconciliation requests a restoration only for details bound to an earlier generation, and a detail retained from before generations existed has none.
- Pool membership removals: `TYCHE_STAGE=<stage> ./enqueue-maintenance.sh RETIRE_MEMBERSHIP <poolId> <gamblerId> <membershipGeneration from OldImage>`, then `RECONCILE_POOL`.

### Rejoin restoration

Every join enqueues `RESTORE_REJOIN_HISTORY` (`RESTORE#<gamblerId>#<membershipGeneration>`), which runs as an ordered pool operation; the flow is in [docs/pool-settlement.md](../../docs/pool-settlement.md#rejoin-restoration). Its failures use the procedures above: a command that keeps failing is retried by the queue, then blocks the pool after 5 claims without progress ([Blocked pool](#blocked-pool)) or reaches the dead-letter queue ([Dead-lettered command](#dead-lettered-command)); a join whose stream record could not be handled is in the failure bucket ([Retained stream failure](#retained-stream-failure)). Rebinding is guarded per detail, so replaying a restoration never counts an award twice.

Inspect a gambler's restoration (read-only): the membership, the restore receipt (`operationState`, `unresolvedDetails`), and every computed detail not bound to the membership, with the reason for any held one:

```bash
TYCHE_STAGE=<stage> ./restore-rejoin-history.sh <poolId> <gamblerId>
```

A detail listed without `heldFor` waits for restoration (the receipt is missing or the operation is still running); one whose `generation` is the earlier membership's and `heldFor` is the current generation was held for repair.

### Held rejoin detail

A restoration holds a retained detail for repair, outside the total, when it is malformed (`computed detail without an award`), its match is missing or has no result, or it reflects a newer result version than its match (`detail reflects a newer result than its match`). The rest of the history is restored.

1. Inspect it: `TYCHE_STAGE=<stage> ./restore-rejoin-history.sh <poolId> <gamblerId>`, then read the full detail and its layout match:

   ```bash
   aws dynamodb get-item --region us-east-2 --table-name Pool --consistent-read \
     --key '{"pk":{"S":"GAMBLER#<gamblerId>#POOL#<poolId>"},"sk":{"S":"MATCH#<matchId>"}}'
   aws dynamodb get-item --region us-east-2 --table-name PoolLayout --consistent-read \
     --key '{"pk":{"S":"POOLLAYOUT#<poolLayoutId>"},"sk":{"S":"MATCH#<matchId>"}}'
   ```

2. Repair the data that caused the hold (for example restore the detail's award from its prediction and the match result, or restore the layout match). Do not change the detail's `poolMembershipGeneration`, and do not change the membership total by hand.
3. Clear the marker of each repaired detail, conditioned on it being held for the current membership: `TYCHE_STAGE=<stage> ./restore-rejoin-history.sh <poolId> <gamblerId> --clear <matchId>`.
4. Replay the restoration: `TYCHE_STAGE=<stage> ./restore-rejoin-history.sh <poolId> <gamblerId> --replay`. It enqueues the command under a new operation ID (`RESTORE#<gamblerId>#<membershipGeneration>#OPERATOR-<time>`), because the first restoration's receipt is already completed. Details restored earlier are skipped; the repaired ones are rebound and their awards added once, and positions are recalculated.
5. Confirm with the inspect command that no computed detail is left unbound. The replay's receipt is `SETTLEMENT#<operation ID printed by --replay>` on the pool partition; it is `COMPLETED` with `unresolvedDetails` 0 when nothing was held again.

### Migration and readiness

Per pool, in order, waiting for the maintenance queue to drain between steps:

```bash
TYCHE_STAGE=<stage> ./enqueue-maintenance.sh INITIALIZE_IDENTITIES --all
TYCHE_STAGE=<stage> ./enqueue-maintenance.sh BACKFILL_PROJECTION --all
TYCHE_STAGE=<stage> ./enqueue-maintenance.sh VERIFY_READINESS --all
```

Readiness is stored on each pool root (`rankingReadiness`). A `NOT_READY` pool with `unresolved > 0` usually has a membership held by identity initialization (its computed awards do not add up to its total); the `IdentityMembersHeld` metric record lists it. Repair the data, then run the three steps again for that pool.

### Kickoff-group backfill

Before the grouped History and Timeline are released, every pool needs the kickoff copy on its computed details and a trend kickoff on its root ([docs/pool-settlement.md](../../docs/pool-settlement.md#kickoff-groups)):

1. Create the index and wait for it to be `ACTIVE`: `TYCHE_STAGE=<stage> ./pool-table.sh ensure-kickoff-index Pool`.
2. Deploy the backend that writes `computedMatchDateTime` and handles `BACKFILL_KICKOFF_GROUPS`.
3. Enqueue the job and wait for the maintenance queue to drain:

   ```bash
   TYCHE_STAGE=<stage> ./enqueue-maintenance.sh BACKFILL_KICKOFF_GROUPS --all
   ```

   Each job copies `matchDateTime` to `computedMatchDateTime` on the pool's computed details that lack it, eight layout matches at a time (found through `GetPoolGamblerScoresByMatch-index`, so details retained from removed memberships are included), and enqueues its continuation. The last job sets the root's `trendKickoffDateTime`, when the root has none, to the kickoff of the match whose `RESULT#` receipt (`SETTLEMENT#RESULT#…` in the pool's partition) was completed last. A pool settled before receipts existed has none; it gets the latest kickoff among the layout matches that have a computed detail in the pool. A pool without computed details keeps no trend kickoff. That write is conditioned on no active operation and on the root's `operationRevision` being the one read, so it never overwrites the value an operation records at completion. While an operation is active the job fails and the queue retries it after the visibility timeout; after 5 receives it is dead-lettered ([Dead-lettered job](#dead-lettered-job)) and can simply be enqueued again. Memberships keep their `beforePosition`. Rerunning the job writes nothing.
4. Verify (read-only scans):

   ```bash
   # Pool roots without a trend kickoff: only pools without computed details (never settled a result) should be listed.
   aws dynamodb scan --region us-east-2 --table-name Pool --projection-expression pk \
     --filter-expression "begins_with(pk, :p) AND pk = sk AND attribute_not_exists(trendKickoffDateTime)" \
     --expression-attribute-values '{":p":{"S":"POOL#"}}'
   # Computed details without the kickoff copy: none should be listed.
   aws dynamodb scan --region us-east-2 --table-name Pool --projection-expression pk,sk \
     --filter-expression "begins_with(pk, :g) AND attribute_exists(computedRequestId) AND attribute_not_exists(computedMatchDateTime)" \
     --expression-attribute-values '{":g":{"S":"GAMBLER#"}}'
   ```

### Reschedule a match

Change the layout match's `matchDateTime` (ISO-8601 UTC) with an `UpdateItem` on `PoolLayout`. The PoolLayout stream copies the new kickoff onto every gambler's detail in every pool using the layout (`matchDateTime`, and `computedMatchDateTime` on computed details), so the match moves to its new kickoff group in History and Timeline; the `KickoffCopiesMoved` metric counts the details written. Copies are only moved when they still hold the previous kickoff, so a redelivered or older change writes nothing. The pool's trend kickoff is not changed. A match rescheduled after it was settled keeps the position snapshots taken under its old group.

The result ingestion is not rescheduled with it. If the match has a `MatchScoreIngestion` item, delete it (its `REMOVE` deletes the old polling schedule; a postponed match may already be `EXPIRED`) and put it again with the new `matchDateTime`, `status` `PENDING`, and no poll fields, so its `INSERT` creates a schedule for the new kickoff.

### Rollout and rollback

| Step | How |
|---|---|
| Quiesce settlement | Deploy with `SettlementProcessingEnabled=false`; stream records (24 h) and queued commands (14 days) are retained. |
| Activate settlement | Deploy with `SettlementProcessingEnabled=true` after identities are initialized. |
| Ranked reads | Available for every pool whose `rankingReadiness` is `READY`. Ranked requests to any other pool answer `503 RANKING_NOT_READY`. |
| Roll back ranked reads | Either the mobile apps stop sending `sort=pool-score-desc`, or mark the affected pools not ready with `TYCHE_STAGE=<stage> ./mark-pool-not-ready.sh <poolId> [reason]`. Requests without `sort` are unaffected. |
| Re-enable ranked reads for a pool | `TYCHE_STAGE=<stage> ./enqueue-maintenance.sh VERIFY_READINESS <poolId>`; the pool answers ranked requests again only if the check records `READY`. |
| Roll back settlement | Keep the versioned writers, receipts, and checkpoints. Pause with `SettlementProcessingEnabled=false`, fix, re-enable, then run `RECONCILE_POOL` for affected pools. Never redeploy the previous unguarded scoring alongside active operations, never delete the original indexes, and never rewrite totals by hand. |

Deploy parameters are passed as `dotnet lambda deploy-serverless --template-parameters "SettlementProcessingEnabled=false"`.

### Capacity

The account's Lambda concurrency limit is 1,000, shared by every function. Settlement (`OnUpdatePositions`) is not capped; it runs at most one invocation per pool with queued commands. Propagation and backfill are capped at 16 concurrent invocations, which propagated one result across a 5,000-member pool in about 210 s. Raise or lower that cap with `--template-parameters "SettlementProcessingEnabled=true;PoolMaintenanceMaximumConcurrency=<n>"` (2 to 1,000). A higher cap finishes propagation sooner, at the cost of more concurrent writes to `Pool` and more of the shared quota. If a function's CloudWatch `Throttles` metric rises, check `aws lambda get-account-settings` before raising any cap.
