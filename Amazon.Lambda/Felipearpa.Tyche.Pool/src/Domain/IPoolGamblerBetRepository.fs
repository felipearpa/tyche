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

    abstract GetFinishedPoolGamblerBetsAsync:
        Ulid * Ulid * string option * string option -> Async<PoolGamblerBet CursorPage>

    abstract GetLivePoolGamblerBetsAsync:
        Ulid * Ulid * string option * string option -> Async<PoolGamblerBet CursorPage>

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
