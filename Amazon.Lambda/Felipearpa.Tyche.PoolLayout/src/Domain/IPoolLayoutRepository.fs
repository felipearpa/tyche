namespace Felipearpa.Tyche.PoolLayout.Domain

open Felipearpa.Core.Paging
open Microsoft.FSharp.Core

type IPoolLayoutRepository =
    abstract GetOpenPoolLayoutsAsync: string option -> Async<PoolLayout CursorPage>
