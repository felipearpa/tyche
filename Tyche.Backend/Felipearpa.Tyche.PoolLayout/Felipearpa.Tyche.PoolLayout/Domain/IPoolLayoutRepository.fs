namespace Felipearpa.Tyche.PoolLayout.Domain

open Felipearpa.Core.Paging
open Microsoft.FSharp.Core

type IPoolLayoutRepository =
    abstract GetOpenedPoolLayoutsAsync: string option -> Async<PoolLayout CursorPage>
