namespace Pipel.Core

[<Interface>]
type ISerializer =

    abstract Serialize<'T> : 'T -> string

    abstract Deserialize<'T> : string -> 'T
