namespace Felipearpa.Tyche.Pool.Domain

open Felipearpa.Core.Paging
open Felipearpa.Type

type IPoolGamblerScoreRepository =

    abstract GetPoolGamblerScoresByGamblerAsync:
        Ulid * string option * string option -> Async<PoolGamblerScore CursorPage>

    abstract GetPoolGamblerScoresByPoolAsync: Ulid * string option * string option -> Async<PoolGamblerScore CursorPage>
