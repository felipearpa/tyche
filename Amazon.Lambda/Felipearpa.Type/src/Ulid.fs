namespace Felipearpa.Type

open System
open Validation

[<AutoOpen>]
module private UlidHelper =

    [<Literal>]
    let regularExpressionForULID = @"^[0123456789ABCDEFGHJKMNPQRSTVWXYZ]{26}$"

[<Struct>]
type Ulid =
    private
    | Ulid of string

    member this.Value =
        match this with
        | Ulid value -> value

    override this.ToString() =
        match this with
        | Ulid value -> value

[<RequireQualifiedAccess>]
module Ulid =

    let private create element =
        match element with
        | Null -> Error "ULID must be not null"
        | NotMatches regularExpressionForULID -> Error $"String {element} must be an ULID format"
        | _ ->
            match Ulid.TryParse element with
            | true, value -> value.ToString() |> Ulid |> Ok
            | _ -> Error $"String {element} has an invalid format"

    let tryOf element =
        match create element with
        | Ok value -> Some value
        | Error _ -> None

    let newOf element =
        match create element with
        | Ok value -> value
        | Error message -> raise <| ArgumentException(message)

    let value (Ulid value) = value

    let toString (element: Ulid) = element.ToString()

    let random () =
        System.Ulid.NewUlid().ToString() |> Ulid
