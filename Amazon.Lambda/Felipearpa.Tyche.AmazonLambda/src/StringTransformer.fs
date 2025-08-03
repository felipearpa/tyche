namespace Felipearpa.Tyche.AmazonLambda

open System

[<AutoOpen>]
module StringTransformer =

    let noneIfEmpty (value: string) : string option =
        if String.IsNullOrWhiteSpace(value) then
            None
        else
            Some value
