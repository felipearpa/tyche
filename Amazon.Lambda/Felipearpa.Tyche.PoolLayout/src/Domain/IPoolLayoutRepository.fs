namespace Felipearpa.Tyche.PoolLayout.Domain

open Felipearpa.Core.Paging
open Felipearpa.Type
open Microsoft.FSharp.Core

type IPoolLayoutRepository =

    abstract GetOpenPoolLayoutsAsync: string option -> Async<PoolLayout CursorPage>

    abstract GetPoolLayoutByIdAsync: Ulid -> Async<PoolLayout option>

    abstract GetPoolLayoutMatchesThroughVersionAsync: Ulid * int * string option -> Async<PoolLayoutMatch CursorPage>
