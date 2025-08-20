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

    abstract AddMatch: InitialPoolGamblerBet -> Result<PoolGamblerBet, AddMatchFailure> Async

    abstract AddMatches: InitialPoolGamblerBet seq -> Async<Result<PoolGamblerBet, AddMatchFailure> seq>
