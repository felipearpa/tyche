namespace Felipearpa.Type

open System
open Felipearpa.Type.Validation

[<AutoOpen>]
module private UuidHelper =

    [<Literal>]
    let uuidRegularExpression =
        @"^[0-9a-fA-F]{8}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{12}$"

[<Struct>]
type Uuid =
    private
    | Uuid of string

    member this.Value =
        match this with
        | Uuid value -> value

    override this.ToString() =
        match this with
        | Uuid value -> value

[<RequireQualifiedAccess>]
module Uuid =

    let private create element =
        match element with
        | Null -> Error "String is null"
        | NotMatches uuidRegularExpression -> Error "String has an invalid format"
        | _ ->
            match Guid.TryParse element with
            | true, value -> value.ToString() |> Uuid |> Ok
            | _ -> Error "String has an invalid format"

    let tryOf element =
        match create element with
        | Ok value -> Some value
        | Error _ -> None

    let newOf element =
        match create element with
        | Ok value -> value
        | Error message -> raise <| ArgumentException(message)

    let value (Uuid value) = value

    let toString (element: Uuid) = element.ToString()

    let random () = Guid.NewGuid().ToString() |> Uuid
