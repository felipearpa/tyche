namespace Felipearpa.Tyche.Pool.Domain

open Felipearpa.Type

/// A readiness evaluation stored on the pool root.
type RankingReadinessReport =
    { Readiness: RankingReadiness
      CheckedMembers: int
      CheckedDetails: int
      Unresolved: int }

/// Data access for membership identity, match-detail identity, and the pool-total
/// projection. Every write is a single guarded operation.
type IPoolProjectionRepository =
    abstract GetMembershipAsync: poolId: Ulid * gamblerId: Ulid -> Async<MembershipState option>

    abstract GetMembershipPageAsync:
        poolId: Ulid * afterGamblerId: string option * limit: int -> Async<MembershipState list * string option>

    /// A strongly consistent page of the member's details in match ID order after the cursor.
    abstract GetBetProjectionPageAsync:
        poolId: Ulid * gamblerId: Ulid * afterMatchId: string option * limit: int ->
            Async<BetProjectionState list * string option>

    /// Gives a membership created before generations existed its generation and initial
    /// score version, unless it already has one.
    abstract InitializeMembershipIdentityAsync:
        poolId: Ulid * gamblerId: Ulid * generation: string -> Async<GuardedWriteOutcome>

    /// Binds an unbound existing detail to the membership generation, conditioned on that
    /// membership still having the generation. Computed details receive result version 1.
    abstract BindDetailIdentityAsync:
        poolId: Ulid * gamblerId: Ulid * matchId: Ulid * generation: string * isComputed: bool ->
            Async<GuardedWriteOutcome>

    /// Copies an absolute total onto an active detail of the same generation whose applied
    /// score version is older.
    abstract ApplyTotalAsync:
        poolId: Ulid * gamblerId: Ulid * matchId: Ulid * binding: MembershipBinding -> Async<GuardedWriteOutcome>

    /// Activates the projection of an existing detail of the same generation, conditioned in
    /// the same transaction on the membership still having the binding's generation and
    /// score version.
    abstract InitializeProjectionAsync:
        poolId: Ulid * gamblerId: Ulid * matchId: Ulid * binding: MembershipBinding -> Async<GuardedWriteOutcome>

    /// Removes the ranking keys of a detail of the given generation and keeps its tombstone.
    abstract DeactivateRankingAsync:
        poolId: Ulid * gamblerId: Ulid * matchId: Ulid * generation: string -> Async<GuardedWriteOutcome>

    /// Deletes an uncomputed detail of the given generation; None targets only details that
    /// were never bound to a generation.
    abstract DeleteUncomputedAsync:
        poolId: Ulid * gamblerId: Ulid * matchId: Ulid * generation: string option -> Async<GuardedWriteOutcome>

    /// Stores readiness when the membership revision is still the one observed by the check.
    abstract SetReadinessAsync:
        poolId: Ulid * report: RankingReadinessReport * observedMembershipRevision: int64 -> Async<bool>
