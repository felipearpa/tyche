namespace Felipearpa.Tyche.Pool.Domain

open Felipearpa.Core.Paging
open Felipearpa.Tyche.Pool.Type
open Felipearpa.Type
open Microsoft.FSharp.Core

type IPoolGamblerScoreRepository =
    abstract GetGamblerScoresAsync: Ulid * string option -> Async<PoolGamblerScore CursorPage>

    abstract GetPoolScoresAsync: Ulid * string option -> Async<PoolGamblerScore CursorPage>

    abstract GetPoolGamblerScoreByIdAsync: Ulid * Ulid -> Result<PoolGamblerScore option, Unit> Async

    /// The trend state of each existing pool root; pools without a root are omitted.
    abstract GetPoolTrendsAsync: poolIds: Ulid list -> Async<PoolTrend list>
