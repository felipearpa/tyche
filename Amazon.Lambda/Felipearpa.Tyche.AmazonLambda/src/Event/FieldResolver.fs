namespace Felipearpa.Tyche.AmazonLambda

open System
open System.Collections.Generic
open Amazon.Lambda.DynamoDBEvents

[<AutoOpen>]
module FieldResolver =

    let tryGetStringFieldOrError
        (key: string)
        (parameters: IDictionary<string, DynamoDBEvent.AttributeValue>)
        : Result<string, string> =
        match parameters.TryGetValue key with
        | true, value ->
            try
                value.S |> Ok
            with :? ArgumentException ->
                Error $"Value {value} at '{key}' failed to satisfy constraint."
        | _ -> Error $"Missing required key '{key}' in parameters."

    let tryGetStringFieldOrNone
        (key: string)
        (parameters: IDictionary<string, DynamoDBEvent.AttributeValue>)
        : string option =
        match parameters |> tryGetStringFieldOrError key with
        | Ok value -> Some value
        | Error _ -> None

    let tryGetIntFieldOrError
        (key: string)
        (parameters: IDictionary<string, DynamoDBEvent.AttributeValue>)
        : Result<int, string> =
        match parameters.TryGetValue key with
        | true, value ->
            match Int32.TryParse(value.N) with
            | true, parsed -> Ok parsed
            | false, _ -> Error $"Value {value} at '{key}' is not a valid integer."
        | _ -> Error $"Missing required key '{key}' in parameters."

    let tryGetIntFieldOrNone
        (key: string)
        (parameters: IDictionary<string, DynamoDBEvent.AttributeValue>)
        : int option =
        match parameters |> tryGetIntFieldOrError key with
        | Ok value -> Some value
        | Error _ -> None
