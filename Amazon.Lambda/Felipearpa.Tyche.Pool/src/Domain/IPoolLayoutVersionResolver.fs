namespace Felipearpa.Tyche.Pool.Domain

open Felipearpa.Type

type IPoolLayoutVersionResolver =
    abstract ResolveAsync: Ulid -> Async<int option>
