namespace Felipearpa.Tyche.Pool.Domain

open System
open Felipearpa.Type

/// One guarded detail settlement or correction together with its membership total.
type SettlementWrite =
    { PoolId: Ulid
      GamblerId: Ulid
      MatchId: Ulid
      OperationId: string
      OperationRevision: int64
      Result: MatchResult
      Change: SettlementChange
      ComputedAt: DateTime }

/// One guarded membership position, paired with the target match detail when the operation
/// settles a match.
type PositionWrite =
    { PoolId: Ulid
      GamblerId: Ulid
      Generation: string
      ExpectedScoreVersion: int64
      ExpectedPositionOperationRevision: int64 option
      OperationRevision: int64
      CalculationRevision: int
      Change: PositionChange
      TargetMatchId: Ulid option }

/// One guarded rebinding of a retained detail together with the rejoined membership's total.
type RebindWrite =
    { PoolId: Ulid
      GamblerId: Ulid
      MatchId: Ulid
      OperationId: string
      OperationRevision: int64
      Change: RebindChange }

/// Marks a retained detail as held for repair against the membership generation, so
/// restoration and reconciliation treat it as resolved until an operator clears the mark.
type RestoreHold =
    { PoolId: Ulid
      GamblerId: Ulid
      MatchId: Ulid
      /// The generation the detail was read with; None for a detail read without one.
      ReadGeneration: string option
      Generation: string
      Reason: string }

/// Data access for ordered pool operations: the pool root checkpoint, settlement receipts,
/// authoritative membership and detail reads, and single guarded writes. It performs no
/// messaging or orchestration.
type IPoolSettlementRepository =
    /// Strongly consistent read of the pool root's ordering state.
    abstract GetRootAsync: poolId: Ulid -> Async<PoolSettlementRoot option>

    abstract GetReceiptAsync: poolId: Ulid * operationId: string -> Async<OperationReceipt option>

    /// Atomically stores the admitted receipt and installs the active checkpoint, provided the
    /// pool has no active operation and its operation revision is still the one observed.
    /// ConditionNotMet means another operation got there first; Contended means a concurrent
    /// transaction touched the root and the same write can be tried again.
    abstract AdmitAsync:
        root: PoolSettlementRoot * receipt: OperationReceipt * active: ActiveOperation -> Async<GuardedWriteOutcome>

    /// Stores a receipt that completes without taking the active fence.
    abstract RecordCompletedAsync: poolId: Ulid * receipt: OperationReceipt -> Async<unit>

    /// Stores a receipt held outside the fence while the pool is blocked.
    abstract RecordDeferredAsync: poolId: Ulid * receipt: OperationReceipt -> Async<unit>

    /// Deferred receipts in the order they were deferred.
    abstract GetDeferredReceiptsAsync: poolId: Ulid -> Async<OperationReceipt list>

    /// Replaces the checkpoint when the stored owner token still matches the expected one.
    /// ConditionNotMet means ownership moved; Contended means the write can be tried again.
    abstract ReplaceCheckpointAsync:
        poolId: Ulid * expectedOwnerToken: string * next: ActiveOperation -> Async<GuardedWriteOutcome>

    /// Completes the receipt and clears the active checkpoint in one transaction, fenced by
    /// the owner token. ConditionNotMet means ownership moved; Contended means the write can
    /// be tried again.
    abstract CompleteAsync: poolId: Ulid * active: ActiveOperation * unresolvedDetails: int -> Async<GuardedWriteOutcome>

    /// A strongly consistent page of memberships in gambler ID order after the cursor.
    abstract GetMembershipPageAsync:
        poolId: Ulid * afterGamblerId: string option * limit: int -> Async<MembershipState list * string option>

    abstract GetMembershipAsync: poolId: Ulid * gamblerId: Ulid -> Async<MembershipState option>

    /// Strongly consistent batched reads of existing details; missing details are omitted.
    abstract GetBetSettlementStatesAsync:
        poolId: Ulid * matchId: Ulid * gamblerIds: Ulid list -> Async<BetSettlementState list>

    abstract GetBetSettlementStateAsync: poolId: Ulid * gamblerId: Ulid * matchId: Ulid -> Async<BetSettlementState option>

    abstract ApplySettlementAsync: write: SettlementWrite -> Async<GuardedWriteOutcome>

    abstract ApplyPositionAsync: write: PositionWrite -> Async<GuardedWriteOutcome>

    /// A strongly consistent page of the gambler's details in the pool, in match ID order
    /// after the cursor.
    abstract GetRetainedBetPageAsync:
        poolId: Ulid * gamblerId: Ulid * afterMatchId: string option * limit: int ->
            Async<RetainedBetState list * string option>

    abstract GetRetainedBetAsync: poolId: Ulid * gamblerId: Ulid * matchId: Ulid -> Async<RetainedBetState option>

    /// Moves a retained detail to the membership generation with its full projection, and adds
    /// its award to the membership total, in one transaction guarded by the detail's read state
    /// and the membership's generation, score version, and total.
    abstract ApplyRebindingAsync: write: RebindWrite -> Async<GuardedWriteOutcome>

    /// Stores the exclusion marker on a retained detail that still has its read generation.
    abstract HoldRetainedBetAsync: hold: RestoreHold -> Async<GuardedWriteOutcome>

    /// A page of distinct pool IDs that have a detail for the match in the match index,
    /// including computed details. The index is eventually consistent.
    abstract GetPoolsWithMatchPageAsync: matchId: Ulid * next: string option -> Async<Ulid list * string option>
