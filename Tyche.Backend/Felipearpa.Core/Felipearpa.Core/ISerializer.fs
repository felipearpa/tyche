namespace Felipearpa.Core

type ISerializer =

    abstract Serialize<'T> : 'T -> string

    abstract Deserialize<'T> : string -> 'T
