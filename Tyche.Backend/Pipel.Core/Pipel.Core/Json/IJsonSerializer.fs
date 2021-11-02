namespace Pipel.Core.Json

[<Interface>]
type IJsonSerializer =

    abstract Serialize<'T> : 'T -> string

    abstract Deserialize<'T> : string -> 'T
