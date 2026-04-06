namespace Felipearpa.Tyche.Pool.Domain

open Felipearpa.Type

type IUpdatePositionsPublisher =
    abstract Publish: Ulid * Ulid -> unit Async
