namespace Felipearpa.Tyche.AmazonLambda

open Felipearpa.Core
open Felipearpa.Core.Json

[<AutoOpen>]
module BodyResolver =

    let tryGetOrError<'Type> (body: string) : Result<'Type, string> =
        let serializer = JsonSerializer() :> ISerializer

        try
            Ok <| serializer.Deserialize<'Type>(body)
        with _ ->
            Error $"Cannot deserialize body into {typeof<'Type>.FullName}"

    let tryGetOrNone<'Type> (body: string) : 'Type option = tryGetOrError body |> Result.toOption
