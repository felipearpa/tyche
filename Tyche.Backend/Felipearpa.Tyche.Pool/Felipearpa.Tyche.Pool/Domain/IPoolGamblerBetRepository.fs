namespace Felipearpa.Tyche.Pool.Data

open Felipearpa.Core.Paging
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Type
open Felipearpa.Type
open Microsoft.FSharp.Core

type BetFailure = | MatchLocked

type IPoolGamblerBetRepository =
    abstract GetPendingPoolGamblerBets: Ulid * Ulid * string option * string option -> Async<PoolGamblerBet CursorPage>
    abstract GetFinishedPoolGamblerBets: Ulid * Ulid * string option * string option -> Async<PoolGamblerBet CursorPage>
    abstract BetAsync: Ulid * Ulid * Ulid * TeamScore<BetScore> -> Result<PoolGamblerBet, BetFailure> Async
