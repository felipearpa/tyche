namespace Felipearpa.Tyche.Pool.Domain

open Felipearpa.Type

/// Data access for the kickoff copies on match details. Every write is a single guarded update.
type IDetailKickoffRepository =
    /// A page of the match's details from the match index, in every pool or only in the given
    /// one. Eventually consistent: discovery only.
    abstract GetMatchDetailsPageAsync:
        matchId: Ulid * poolId: Ulid option * next: string option -> Async<DetailKickoff list * string option>

    /// Strongly consistent read of one detail's kickoff copies.
    abstract GetDetailAsync: poolId: Ulid * gamblerId: Ulid * matchId: Ulid -> Async<DetailKickoff option>

    /// Writes the planned copies when the detail still holds the expected ones; ConditionNotMet
    /// otherwise, including when the detail no longer exists.
    abstract RewriteKickoffAsync: write: KickoffCopyWrite -> Async<GuardedWriteOutcome>
