namespace Felipearpa.Tyche.Pool.Domain

open Felipearpa.Core.Paging
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Type
open Felipearpa.Type
open Microsoft.FSharp.Core

type BetFailure = | MatchLocked

/// Outcome of creating a match detail bound to a membership.
type MaterializationOutcome =
    | Materialized
    | AlreadyMaterialized
    /// The membership was removed or its generation or score version changed after the
    /// binding was read.
    | BindingChanged

type IPoolGamblerBetRepository =
    abstract GetPendingPoolGamblerBetsAsync:
        Ulid * Ulid * string option * string option -> Async<PoolGamblerBet CursorPage>

    abstract GetLivePoolGamblerBetsAsync:
        Ulid * Ulid * string option * string option -> Async<PoolGamblerBet CursorPage>

    /// A storage page of the gambler's computed details by kickoff, newest kickoff first.
    abstract GetFinishedBetsByKickoffAsync:
        poolId: Ulid * gamblerId: Ulid * next: string option -> Async<GroupedBet CursorPage>

    /// Every computed detail of the gambler at the stored kickoff, in the paged order, with
    /// the continuation that resumes that traversal after the last of them (None when there is
    /// none).
    abstract GetFinishedBetsAtKickoffAsync:
        poolId: Ulid * gamblerId: Ulid * storedKickoff: string -> Async<GroupedBet list * string option>

    /// A storage page of the gambler's uncomputed details whose predictions are revealed,
    /// oldest kickoff first.
    abstract GetUnsettledBetsByKickoffAsync:
        poolId: Ulid * gamblerId: Ulid * next: string option -> Async<GroupedBet CursorPage>

    /// Every detail of the gambler at the stored kickoff, computed or not, in the unsettled
    /// traversal's order, with the continuation that resumes that traversal after the last of
    /// them (None when there is none).
    abstract GetBetsAtKickoffAsync:
        poolId: Ulid * gamblerId: Ulid * storedKickoff: string -> Async<GroupedBet list * string option>

    abstract GetPoolMatchGamblerBetsAsync:
        Ulid * Ulid * string option -> Async<PoolGamblerBet CursorPage>

    abstract GetPoolGamblerBetByIdAsync: Ulid * Ulid * Ulid -> Async<PoolGamblerBet option>

    abstract BetAsync: Ulid * Ulid * Ulid * TeamScore<BetScore> -> Result<PoolGamblerBet, BetFailure> Async

    /// Creates the detail unless it exists, together with its identity and pool-total
    /// projection, conditioned on the membership still matching the binding. Without a
    /// binding the detail is created unbound for a membership whose identity is not
    /// initialized yet.
    abstract MaterializeBetAsync: InitialPoolGamblerBet * MembershipBinding option -> Async<MaterializationOutcome>

    /// Records that the membership has every match through the layout version.
    abstract RaisePoolLayoutVersionAsync: poolId: Ulid * gamblerId: Ulid * poolLayoutVersion: int -> Async<unit>
