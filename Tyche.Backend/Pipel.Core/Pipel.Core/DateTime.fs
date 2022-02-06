namespace Pipel.Core

open System

module DateTimeExtensions =

    type DateTime with

        member this.ToISOString() =
            this.ToString("yyyy-MM-ddTHH:mm:ss.fffzzz")
