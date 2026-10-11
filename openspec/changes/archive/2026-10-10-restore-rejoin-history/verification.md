# Verification

## Production — task 2.1 (2026-10-10)

### Deploy

The user ran `Amazon.Lambda/deploy-lambda.sh`. Stack `TycheApi` (us-east-2) reached `UPDATE_COMPLETE` at 2026-10-10T23:27:30Z with `SettlementProcessingEnabled=true` and `PoolMaintenanceMaximumConcurrency=16`. Every function runs the same package (CodeSha256 `A1isuZAt…`, `OnMembershipChange` LastModified 23:28:04Z). Its `Felipearpa.Tyche.Pool.dll` and `Felipearpa.Tyche.AmazonLambda.dll` contain `RESTORE_REJOIN_HISTORY`, `restoreExcludedGeneration`, `restoredDetails`, and `RestorePass`.

Aborted earlier attempt: the update at 20:46:24Z deployed a build from 20:45Z, before the restore code existed. A rehearsal started on it at 23:23Z. The joins queued no restore commands, and the downloaded package had no restore code, so the attempt was stopped before any bet or result. Its data was created 23:23–23:24Z: 3 accounts, a layout with 5 matches, and pool `01M4M224HPRXWCNAF1S92BTGB8` with 3 members, 15 details, and 6 standings receipts. All of it was deleted by 23:27:22Z, and the check in [2.1-12-cleanup-check.txt](verification/2.1-12-cleanup-check.txt) covers its IDs too.

### Rehearsal data

All data was rehearsal-owned, with fresh IDs:

- Accounts A `01M4M2DEEATJD6GCX6G7BGPM2J` (owner), B `01M4M2DEECR8QQX3S5Z91KXPMM` (the rejoiner), and C `01M4M2DEEFDRKK8WE6NPPFHXQ4`. Each has an `ACCOUNT#` item and an `EMAIL#…@rehearsal.invalid` item.
- Layout `01M4M2DEE789N494PCBZGZJ5AY`. Its root has no `status`, so it never appeared in the open-layout list.
- Matches M1 `…4BW0`, M2 `…5C9J`, M3 `…PQVM`, M5 `…HFF8` (kickoff 23:49Z) and M4 `…91SX` (open, kickoff 2027-06-01).
- Pool `01M4M2DPY0DXJ4CAGKZVJ322RR`, created through `CreatePool`. B and C joined through `JoinPool`.

API calls went to the deployed functions with `aws lambda invoke` and the HTTP API v2 request shape: `CreatePool`, `JoinPool`, `Bet`, and `GetMatchBets` with an email JWT claim, plus `RemoveGambler` with the owner's claim. Results were written with the `SetMatchFinalScore` update shape: score plus `resultVersion = 1`, conditioned on no score. Corrections used `operations/correct-result.sh`. The account rows were first written with mixed-case emails, which `Email` lowercases, so the first bets answered 401; the rows were rewritten in lowercase before any bet was accepted.

Predictions (all placed through `Bet`, 200):

| Match | Result | A | B | C |
|---|---|---|---|---|
| M1 | 2-1, corrected to 0-0 while B was away | 2-1 | 3-0 | 0-1 |
| M2 | 1-0, corrected to 1-3 after restoration | 0-0 | 1-3 | 2-0 |
| M5 | 1-1 | 0-2 | 1-1 | 2-2 |
| M3 | 3-0, scored while B was away | 3-0 | 2-0 (deleted at removal) | 1-0 |
| M4 | open | — | 1-1 (deleted at removal), 2-2 after rejoining | — |

### Steps and results

| Step (UTC) | Action | Result |
|---|---|---|
| 23:30 | Create the pool; B and C join | Each join completed a `RESTORE#<gambler>#<generation>` receipt with no changes (`SettledDetails` 0) and a standings receipt |
| 23:31:06–10 | Publish M1 2-1, M2 1-0, M5 1-1 | Each `RESULT#…#1` receipt completed within 1 s. B 34 (10 + 4 + 20), C 32, A 24; positions B 1, C 2, A 3 ([01](verification/2.1-01-settled-before-removal.json)) |
| 23:31:46 | A removes B (`RemoveGambler`, 204) | B's M1, M2, and M5 details kept their awards as tombstones: generation `…YAK6`, `rankingActive=false`, no ranking keys, `poolScoreVersion` 4. M3 and M4 (uncomputed) were deleted |
| 23:32:48 | `./correct-result.sh <layout> M1 0 0 1` | `RESULT#M1#2` completed; A 24 → 4, C 32 → 30. B's M1 tombstone stayed at 10 with result version 1 |
| 23:32:50 | Publish M3 3-0 | A 24, C 44 |
| 23:33:16 | Remove `score` from B's M5 tombstone (conditioned on award 20 and generation `…YAK6`) | M5 is now a malformed retained detail ([02](verification/2.1-02-during-absence.json)) |
| 23:34:04 | B rejoins (`JoinPool`, 204), new generation `01M4M2N7XFGC6YB54Y05E8HPW4` | `RESTORE#B#…HPW4` completed at 23:34:05 with `SettledDetails` 2, `UnresolvedDetails` 1 ([03](verification/2.1-03-after-restore.json), [metrics](verification/2.1-04-restore-metrics.jsonl)) |
| 23:34:46 | `./restore-rejoin-history.sh <pool> <B>` | Lists only M5, `heldFor` = the new generation, reason `computed detail without an award` ([05](verification/2.1-05-inspect-held.txt)) |
| 23:34:53–57 | Repair M5 (`score = 20`: 1-1 against 1-1); `--clear M5`; a second `--clear` (refused, `ConditionalCheckFailedException`); `--replay` | `--replay` enqueued `RESTORE#B#…HPW4#OPERATOR-20261010T233457Z` ([06](verification/2.1-06-clear-and-replay.txt)) |
| 23:35:14 | Replay completed | `SettledDetails` 1, `unresolvedDetails` 0. The inspect command lists no unbound computed detail ([07](verification/2.1-07-after-replay.json)) |
| 23:35:41 | Resend the original command `RESTORE#B#…HPW4` | `CommandAlreadyCompleted`; B stays at 28, score version 4 |
| 23:35:59 | `./enqueue-maintenance.sh VERIFY_READINESS <pool>` (rehearsal pool only) | `READY`, 3 members, 15 details, unresolved 0 ([08](verification/2.1-08-readiness.json)) |
| 23:36 | `GetMatchBets` ranked request for M1, M2, and M5 | 200 ([09](verification/2.1-09-ranked-after-restore.json)) |
| 23:36:42 | `./correct-result.sh <layout> M2 1 3 1` | `RESULT#M2#2` completed with `SettledDetails` 3 ([10](verification/2.1-10-after-correction.json)) |

[11](verification/2.1-11-receipts.json) lists all 16 receipts in revision order. None yielded, failed, or was blocked.

### Checks against the spec

- **Restored total and the corrected award.** After the restore, B's M1 detail was re-evaluated against result version 2 (3-0 against 0-0: 4 points, `resultVersion` 2). M2 kept 4 points. B's new membership total was 8 = 4 + 4, with score version 1 → 3, one advance per rebound detail. It was not 14, because M1 had been corrected while B was away.
- **Held detail.** M5 was held and excluded from the total. After the repair and replay, it was rebound once: B 8 → 28 (score version 4). The duplicate original command changed nothing.
- **Late join and deleted bets.** M3, scored during the absence, got a late-join detail with no prediction and 0 points. The deleted M4 bet was not recreated (no prediction after rejoining), and B could bet M4 again (2-2, 200).
- **Current positions.** After the restore: C 1, A 2, B 3. After the replay: C 1, B 2, A 3. `beforePosition` stayed at A 2 and C 1 through both, and the rejoined B has none.
- **Historical positions.** B's restored details kept their recorded match positions: M1 2/2, M2 3/2, M5 1/3 (`position`/`beforePosition`), identical to the capture before removal. Predictions and `computedDateTime` were unchanged.
- **Projection.** After propagation, every B detail of the new generation carried the membership total and version: 8 at version 3 after the restore, 28 at version 4 after the replay, 44 at version 5 after the correction. Every detail had `rankingActive=true` and both ranking keys, even though the tombstones had held `poolScoreVersion` 4, above the new membership's starting version.
- **Ranked list of an older match.** M1 after the replay: C (44, prediction 0-1, award 4), B (28, prediction 3-0, award 4), A (24, prediction 2-1, award 0). B appears with the original prediction and the restored award at the position its total gives. M5 lists B with 1-1 and 20 points.
- **Correction after restoration.** M2 1-0 → 1-3 moved B 28 → 44 (+16, the award difference 20 − 4) with one score-version advance (4 → 5), `beforeScore` 28. A went 24 → 20 and C 44 → 32. The ranked lists for M1 and M2 then put B first (44).

### Cleanup

At 23:37:24Z the three membership rows were deleted. The removal handlers ran 3 standings operations and deleted the uncomputed details, then both queues drained. Then deleted:

- 12 remaining computed details
- the pool root and its 19 receipts (`RESTORE`, `RESULT`, `STANDINGS`)
- 5 layout matches and the layout root
- 6 Account items

Cleanup finished at 23:38:37Z.

Check at 23:39:57Z ([12](verification/2.1-12-cleanup-check.txt)), for both this run's IDs and the aborted run's:

- 0 items in every pool, detail, account, email, and layout partition.
- 0 rows in `GetGamblersByPoolLayout-index`, `GetPoolGamblerScoresByMatch-index`, and `GetPoolMatchBetsByTotal-index` for those keys.
- 0 MatchScoreIngestion items naming the matches.

### Final production state

- **Queues:** `UpdatePositionsQueue.fifo`, `UpdatePositionsQueue-dlq.fifo`, `PoolMaintenanceQueue`, and `PoolMaintenanceQueue-dlq` all empty (0 visible, in flight, and delayed).
- **Failure bucket:** `tyche-stream-failures-567776770335` empty.
- **Alarms:** all nine `TycheApi-*` OK.
- **Event-source mappings:** all six enabled, `OnPoolMaintenanceJob` at `MaximumConcurrency` 16.
- **Stack:** `UPDATE_COMPLETE` at 23:27:30Z, parameters unchanged.
- **Metrics:** `CommandFailures`, `JobFailures`, `StreamRecordFailures`, `OperationBlocked`, and `OperationYielded` were 0 from 23:28Z to 23:41Z, and the five event handlers had no Lambda errors.
- **Real data:** no real pool, gambler, account, or layout was read or written.

### Not verified

- The rejoin check in `Amazon.Lambda/Felipearpa.Tyche.Verification/Integration.fs` was not run; it needs isolated AWS resources.
- Ranked requests went to the function directly with the JWT claim shape, not through API Gateway with a real Firebase token.

Accepted without the ranked endpoint through API Gateway with a real Firebase token by the user on 2026-10-10.
Accepted without the rejoin check in `Amazon.Lambda/Felipearpa.Tyche.Verification/Integration.fs` on isolated AWS resources by the user on 2026-10-10.
