namespace Felipearpa.Tyche.AmazonLambda

open System
open System.Collections.Generic

[<AutoOpen>]
module ParameterResolver =

    let tryGetStringParamOrError (key: string) (parameters: IDictionary<string, string>) : Result<string, string> =
        match parameters.TryGetValue key with
        | true, value ->
            try
                Convert.ChangeType(value, typeof<string>) :?> string |> Ok
            with
            | :? InvalidCastException
            | :? ArgumentException -> Error $"Value {value} at '{key}' failed to satisfy constraint."
        | _ -> Error $"Missing required key '{key}' in parameters."

    let tryGetStringParamOrNone (key: string) (parameters: IDictionary<string, string>) : string option =
        match parameters |> tryGetStringParamOrError key with
        | Ok value -> Some value
        | Error _ -> None

    let tryGetIntParamOrError (key: string) (parameters: IDictionary<string, string>) : Result<int, string> =
        match parameters.TryGetValue key with
        | true, value ->
            match Int32.TryParse(value) with
            | true, parsed -> Ok parsed
            | false, _ -> Error $"Value {value} at '{key}' is not a valid integer."
        | _ -> Error $"Missing required key '{key}' in parameters."

    let tryGetIntParamOrNone (key: string) (parameters: IDictionary<string, string>) : int option =
        match parameters |> tryGetIntParamOrError key with
        | Ok value -> Some value
        | Error _ -> None
