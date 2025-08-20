namespace Felipearpa.Tyche.Pool.Domain

open Felipearpa.Core.Paging
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Type
open Felipearpa.Type
open Microsoft.FSharp.Core

type BetFailure = | MatchLocked

type AddMatchFailure = | AlreadyExist

type IPoolGamblerBetRepository =
    abstract GetPendingPoolGamblerBetsAsync:
        Ulid * Ulid * string option * string option -> Async<PoolGamblerBet CursorPage>

    abstract GetFinishedPoolGamblerBetsAsync:
        Ulid * Ulid * string option * string option -> Async<PoolGamblerBet CursorPage>

    abstract BetAsync: Ulid * Ulid * Ulid * TeamScore<BetScore> -> Result<PoolGamblerBet, BetFailure> Async

    abstract AddPoolGamblerMatchAsync: InitialPoolGamblerBet -> Result<PoolGamblerBet, AddMatchFailure> Async

    abstract AddPoolGamblerMatchesAsync: InitialPoolGamblerBet seq -> Async<Result<PoolGamblerBet, AddMatchFailure> seq>
