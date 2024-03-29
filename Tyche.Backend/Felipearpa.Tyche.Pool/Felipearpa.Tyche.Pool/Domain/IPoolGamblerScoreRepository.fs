namespace Felipearpa.Tyche.Pool.Domain

open Felipearpa.Core.Paging
open Felipearpa.Type
open Microsoft.FSharp.Core

type IPoolGamblerScoreRepository =
    abstract GetGamblerScoresAsync: Ulid * string option * string option -> Async<PoolGamblerScore CursorPage>

    abstract GetPoolScoresAsync: Ulid * string option * string option -> Async<PoolGamblerScore CursorPage>

    abstract GetPoolGamblerScoreAsync: Ulid * Ulid -> Result<PoolGamblerScore option, Unit> Async
