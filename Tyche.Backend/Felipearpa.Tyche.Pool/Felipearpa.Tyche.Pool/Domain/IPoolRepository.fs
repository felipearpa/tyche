namespace Felipearpa.Tyche.Pool.Domain

open Felipearpa.Type

type IPoolRepository =

    abstract GetPoolAsync: Ulid -> Async<Pool option>
