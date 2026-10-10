# Proposal

## Why

When a pool owner removes a gambler and that gambler later rejoins, the new membership starts at zero points. Their retained computed bets stay attached to the removed membership, so the gambler loses the points they earned and disappears from the ranked participant lists of the matches they played. A rejoining gambler should continue with their earned history.

## What Changes

- When a removed gambler rejoins a pool, always restore their history; the owner does not choose.
- Re-link the gambler's retained computed match bets from earlier memberships to the new membership, and add each retained award to the new membership total exactly once.
- Keep predictions, results, match awards, and historical match positions unchanged.
- Recalculate current pool positions after restoration, and propagate the restored total to every match row so the gambler appears again in older matches' ranked lists.
- Make later result corrections on restored matches adjust a total that includes the original award.
- Do not recreate bets deleted at removal. Open matches can be bet on again; matches scored while the gambler was absent keep the existing late-join rule of zero points.
- Run restoration through the existing ordered, durable per-pool operation flow so it is safe under retries, duplicate delivery, concurrent settlement, and another removal.
- **BREAKING (behavior):** replaces the current rejoin behavior, under which the new membership starts at zero and retained bets remain outside the ranked lists.

## Capabilities

### New Capabilities

- `pool-rejoin-history`: Restoring a rejoining gambler's retained bets and earned points in a pool, including exactly-once totals, ranked-list visibility in older matches, correction consistency, and safety under retries, settlement, and repeated removal.

### Modified Capabilities

None. No existing specification under `openspec/specs/` defines the rejoin rule. The in-progress `redesign-match-bets` change keeps the previous rejoin behavior; this change replaces it after that change is applied.

## Impact

- Backend only: F# Pool application operations, the join flow, settlement repository writes, and pool operation command handling in Lambda; the existing Pool table, its stream, and the per-pool SQS FIFO queue. No new tables, indexes, or queues.
- Depends on the in-progress, uncommitted `redesign-match-bets` change: membership generations, score versions, operation receipts and checkpoints, ordered pool operations, total propagation, and the ranked match-bets index.
- Documentation: `docs/database-model.md`, `docs/pool-settlement.md`, and the operator runbook in `Amazon.Lambda/operations/README.md`.
- No mobile, API contract, or migration changes. Production currently has 266 retained bets from 11 removed gamblers in 2 pools, and none of these gamblers has rejoined; restoration applies when a rejoin next occurs.
