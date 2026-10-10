# Design

## Context

See [proposal.md](proposal.md) for motivation and [specs/pool-rejoin-history/spec.md](specs/pool-rejoin-history/spec.md) for the required behavior.

This change builds on `redesign-match-bets`, which introduces the mechanisms reused here (see `docs/pool-settlement.md` and `docs/database-model.md` once that change is applied):

- Each membership has a unique `membershipGeneration` and a monotonic `scoreVersion` that advances with every change of its total.
- Each match detail (`GAMBLER#<gamblerId>#POOL#<poolId>` / `MATCH#<matchId>`) records `poolMembershipGeneration`, `resultVersion`, settlement markers, the copied total, and ranking keys. Writes to a detail require its generation to match the membership's.
- Per-pool operations (settlement, corrections, standings reconciliation) run one at a time through the pool's FIFO queue under an active-operation fence, with durable receipts and resumable checkpoints.
- Removal deletes the membership and the gambler's uncomputed details, and keeps computed details as inactive tombstones of the removed generation. Details from before generations existed carry no generation.
- A join creates a new generation. Join materialization creates late-join details for matches without a detail and skips any key that already exists, so retained details occupy their match keys and stay bound to an earlier generation.
- Result corrections apply only to details of the current generation. A retained detail can therefore hold an award from before a correction made while the gambler was absent.
- Total propagation copies a membership's absolute total to every active detail of its generation whose copied version is older, and sets the ranking keys.

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

The membership stream handler already reacts to a new membership by requesting standings reconciliation. For a membership insert, it also durably enqueues a `RESTORE_REJOIN_HISTORY` command before acknowledging the stream record. The command's stable identity is derived from pool, gambler, and the new generation. Every join requests it; an operation that finds no retained details completes as a no-op. This avoids detecting rejoins from incomplete signals, such as the absence of a previous membership record.

**Alternative:** Restoring inside the join transaction cannot cover an unbounded number of details atomically and would bypass pool ordering.

### 2. Restore runs as an ordered pool operation

The command uses the same admission, receipt, active-operation fence, checkpoint, deadline yield, and dead-letter handling as settlement. Settlement, corrections, and position calculation for the pool therefore cannot interleave with a restore pass. Duplicate delivery of a completed restore is a no-op through its receipt.

### 3. One guarded transaction per retained detail

The operation strongly reads the gambler's base detail partition for the pool and selects computed details that do not belong to the new generation. Those are details with a different generation or with no generation. For each detail, one transaction:

- updates the detail: sets the new generation, marks it active for ranking, and leaves its copied-total version below the membership's so normal propagation sets the total and ranking keys. It records the restore operation as the detail's latest applied operation.
- updates the membership: adds the detail's award to `score` and advances `scoreVersion`.

Conditions require that the detail still lacks the new generation and is computed, and that the membership exists, is active, and has the new generation. A retry after a lost response fails the detail condition; the operation then reads the detail, confirms it already belongs to the new generation, and records the outcome as applied. Each retained detail can be rebound only once, so details from several earlier generations each contribute exactly once. A gambler removed again fails the membership condition: the operation stops rebinding, records the remaining details as deliberately excluded, and completes. The details stay retained for a later rejoin.

**Alternative:** Summing all retained awards first and writing the total once would need a separate marker per detail to stay idempotent, and a partial failure would leave the total and the bound details inconsistent.

### 4. Re-evaluate awards against the current result

Before rebinding, the operation reads the match's authoritative result and result version, as settlement does. If the detail's `resultVersion` is older, the transaction also writes the award from the current domain evaluator and the current result version. The membership receives that award. A detail that already reflects the current result keeps its award. The scoring formula is unchanged.

### 5. Standings and propagation follow the existing paths

After all details have an outcome, the operation runs a current-standings position calculation within the same operation, before completion. It updates only membership positions; restored details keep their recorded historical positions. The `scoreVersion` changes then trigger normal total propagation, which copies the final total to all active details of the generation, including the restored ones, and sets their ranking keys. Rows appear in ranked lists once propagation and the index converge.

### 6. Corrections after restoration need no special handling

A restored detail belongs to the current generation and carries its current award and result version. A later correction applies its award difference to a total that already includes that award, using the existing correction path.

## Risks / Trade-offs

- [The stream handler acknowledges a join before the restore command is accepted] → Enqueue before acknowledging, with the stream's existing retry and failure retention; standings reconciliation can also request a missing restore for a membership whose retained details remain unbound.
- [A large retained history makes one pass long] → Bound each chunk and use the existing checkpoint and deadline yield; a gambler has at most one detail per match, so the work per restore is bounded by the pool's match count.
- [Restore overlaps a newer membership change] → Generation conditions on every write, and membership-revision validation for the standings step as in settlement.
- [Behavior change surprises owners who expected a fresh start] → The proposal records it as a deliberate behavior change; the rule applies to every rejoin.
- [A retained detail is malformed or its match is missing] → Hold it for repair, exclude it from the total, and surface it through reconciliation metrics rather than guessing an award.

## Migration Plan

1. Deploy the new command handling and stream behavior together. No data migration is needed; existing retained details stay inactive until their gambler rejoins.
2. Rehearse on a temporary production pool: join, settle, remove, correct a result during the absence, rejoin, then verify the total, positions, ranked lists, and a later correction; delete the temporary data afterwards.
3. Rollback: redeploy the previous code. Restores that already completed remain consistent, because their details and totals were written together; pending restore commands remain in the queue or dead-letter queue until the code is redeployed or the commands are purged.
