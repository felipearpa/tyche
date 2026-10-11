# Design

## Context

See [proposal.md](proposal.md) for motivation and [specs/pool-rejoin-history/spec.md](specs/pool-rejoin-history/spec.md) for the required behavior.

This change builds on `redesign-match-bets` (archived 2026-10-10), which introduced the mechanisms reused here (see `docs/pool-settlement.md` and `docs/database-model.md`):

- Each membership has a unique `membershipGeneration` and a monotonic `scoreVersion` that starts at 1 for every membership, including a rejoin, and advances with every change of its total.
- Each match detail (`GAMBLER#<gamblerId>#POOL#<poolId>` / `MATCH#<matchId>`) records `poolMembershipGeneration`, `resultVersion`, settlement markers, the copied total, and ranking keys. Writes to a detail require its generation to match the membership's.
- Per-pool operations (settlement, corrections, standings reconciliation) run one at a time through the pool's FIFO queue under an active-operation fence, with durable receipts and resumable checkpoints.
- Removal deletes the membership and the gambler's uncomputed details, and keeps computed details as inactive tombstones of the removed generation. A tombstone keeps the `poolScoreVersion` it last copied from the removed membership. Details from before generations existed carry no generation and usually no `poolScoreVersion`.
- A join creates a new generation. Join materialization creates late-join details for matches without a detail and skips any key that already exists, so retained details occupy their match keys and stay bound to an earlier generation.
- Result corrections apply only to details of the current generation. A retained detail can therefore hold an award from before a correction made while the gambler was absent.
- Total propagation copies a membership's absolute total to every active detail of its generation whose copied version is older, and sets the ranking keys. Its condition (`poolScoreVersion < :version`) fails for a detail whose `poolScoreVersion` is missing or not below the membership's.
- A standings reconciliation (`STANDINGS#<source>`) writes current positions and leaves `beforePosition` unchanged; only the first position write of a match settlement or correction records `beforePosition`.

## Goals / Non-Goals

**Goals:**

- Restore retained history through the existing ordered pool operation flow, with no new table, index, queue, or service.
- Make every rebinding self-guarding, so exactly-once accounting does not depend on operation-level bookkeeping alone.

**Non-Goals:**

- Restoring uncomputed bets deleted at removal, or changing the late-join rule.
- Rewriting historical match positions or `beforePosition` values.
- Owner-facing controls, mobile changes, or API contract changes.
- Migrating existing retained rows ahead of a rejoin.

## Decisions

### 1. A rejoin requests a dedicated restore operation

The membership stream handler already reacts to a new membership by requesting standings reconciliation. For a membership insert, it also durably enqueues a `RESTORE_REJOIN_HISTORY` command before acknowledging the stream record. The command's stable identity is `RESTORE#<gamblerId>#<membershipGeneration>` within the pool, a new `operationId` form alongside `RESULT#`, `MATCH#`, and `STANDINGS#`. Because a completed receipt makes that ID a no-op, an operator replay uses `RESTORE#<gamblerId>#<membershipGeneration>#<source>`, following the `MATCH#<matchId>#<source>` pattern; the per-detail guards make a second pass safe. Every join requests it; an operation that finds no retained details completes as a no-op. This avoids detecting rejoins from incomplete signals, such as the absence of a previous membership record.

**Alternative:** Restoring inside the join transaction cannot cover an unbounded number of details atomically and would bypass pool ordering.

### 2. Restore runs as an ordered pool operation

The command uses the same admission, receipt, active-operation fence, checkpoint, deadline yield, and dead-letter handling as settlement. Settlement, corrections, and position calculation for the pool therefore cannot interleave with a restore pass. Duplicate delivery of a completed restore is a no-op through its receipt.

### 3. One guarded transaction per retained detail

The operation strongly reads the gambler's base detail partition for the pool and selects computed details that do not belong to the new generation and are not excluded for repair (decision 7). Those are details with a different generation or with no generation. For each detail, one transaction:

- updates the detail: writes the full projection for the new binding, as `initializeProjection` does: the new generation, `rankingActive = true`, both ranking keys, and the membership's post-transaction total and `scoreVersion` as `totalPoolPoints` / `poolScoreVersion`. Leaving the tombstone's copy state for propagation is not enough. Its `poolScoreVersion` came from the removed membership and can exceed the new membership's version, which restarts at 1, and a legacy detail may have none; either makes propagation's `poolScoreVersion < :version` condition skip the detail. Removal also deleted `getPoolMatchBetsByTotalPk`, which `applyTotal` never sets. Details restored earlier in the same pass then hold an older copy, and the next propagation updates them normally. The transaction records the restore operation as the detail's latest applied operation.
- updates the membership: adds the detail's award to `score` and advances `scoreVersion`. It is guarded by the generation, the expected `scoreVersion`, and the previous total, as in settlement, so the copied total is exact.

Conditions require that the detail is computed and still has the generation the operation read: the expected earlier generation value, or `attribute_not_exists` for a detail read without one. Requiring the exact read value, not merely "not the new generation", keeps a rebinding from overwriting a detail that another writer changed after the read. The membership must exist, be active, and have the new generation. A membership condition failure is re-read and classified as in settlement: a removed or replaced membership stops the pass, and a changed `scoreVersion` retries with the fresh value. A retry after a lost response fails the detail condition; the operation then reads the detail, confirms it already belongs to the new generation, and records the outcome as applied. Each retained detail can be rebound only once, so details from several earlier generations each contribute exactly once. A gambler removed again fails the membership condition: the operation stops rebinding, records the remaining details as deliberately excluded, and completes. The details stay retained for a later rejoin.

**Alternative:** Summing all retained awards first and writing the total once would need a separate marker per detail to stay idempotent, and a partial failure would leave the total and the bound details inconsistent.

### 4. Re-evaluate awards against the current result

Before rebinding, the operation reads the match's authoritative result and result version, as settlement does. If the detail's `resultVersion` is older, the transaction also writes the award from the current domain evaluator and the current result version. The membership receives that award. A detail that already reflects the current result keeps its award. The scoring formula is unchanged.

### 5. Standings and propagation follow the existing paths

After all details have an outcome, the operation runs a current-standings position calculation within the same operation, before completion, but only when it rebound at least one detail; the checkpoint carries the restored count across yields. A restore that rebound nothing completes without a position pass, since the join's own standings command already placed the membership. It follows the standings-reconciliation rule: it writes current membership positions and leaves every membership's `beforePosition` unchanged, so a restore does not count as a match movement in any gambler's trend. Restored details keep their recorded historical positions. The `scoreVersion` changes then trigger normal total propagation, which copies the final total to all active details of the generation, including restored ones whose copy is older, and updates their ranking sort keys. Rows appear in ranked lists once propagation and the index converge.

### 6. Corrections after restoration need no special handling

A restored detail belongs to the current generation and carries its current award and result version. A later correction applies its award difference to a total that already includes that award, using the existing correction path.

### 7. Details held for repair carry an exclusion marker

A retained detail that is malformed or whose match is missing is not rebound and not counted. The operation writes `restoreExcludedGeneration = <new generation>` and `restoreExclusionReason` on it, conditioned on the same read generation, and records it in the operation's `unresolvedDetails`. Selection (decision 3) and reconciliation (decision 1 risk) treat a detail whose `restoreExcludedGeneration` equals the membership's generation as resolved, so neither requests another restore for it. After repair, an operator clears the marker and replays the restore command, as documented in the runbook. A later rejoin has a new generation, so the old marker does not block it.

## Risks / Trade-offs

- [The stream handler acknowledges a join before the restore command is accepted] → Enqueue before acknowledging, with the stream's existing retry and failure retention; standings reconciliation can also request a missing restore for a membership with computed retained details of an earlier generation that are not excluded for it.
- [A large retained history makes one pass long] → Bound each chunk and use the existing checkpoint and deadline yield; a gambler has at most one detail per match, so the work per restore is bounded by the pool's match count.
- [Restore overlaps a newer membership change] → Generation conditions on every write, and membership-revision validation for the standings step as in settlement.
- [Behavior change surprises owners who expected a fresh start] → The proposal records it as a deliberate behavior change; the rule applies to every rejoin.
- [A retained detail is malformed or its match is missing] → Hold it for repair with an exclusion marker (decision 7), exclude it from the total, and surface it through reconciliation metrics rather than guessing an award.
- [Reconciliation and details with no generation] → Reconciliation does not request a restore for computed details without a generation: a membership held unbound by identity initialization can have such details already counted in its total, and restoring them would double-count. A rejoin's legacy details are still restored by the restore its insert requests; an operator replaying a failed insert record replays the restore explicitly.
- [Stale `beforePosition` after a restore] → Accepted: trends keep describing the latest match movement, as after any standings reconciliation.

## Migration Plan

1. Deploy the new command handling and stream behavior together. No data migration is needed; existing retained details stay inactive until their gambler rejoins.
2. Rehearse on a temporary production pool: join, settle, remove, correct a result during the absence, rejoin, then verify the total, positions, ranked lists, and a later correction; delete the temporary data afterwards.
3. Rollback: redeploy the previous code. Restores that already completed remain consistent, because their details and totals were written together; pending restore commands remain in the queue or dead-letter queue until the code is redeployed or the commands are purged.
