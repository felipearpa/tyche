namespace Felipearpa.Type

open System
open Felipearpa.Type.Validation

[<Struct>]
type NonEmptyString100 =
    private
    | NonEmptyString100 of string

    member this.Value =
        match this with
        | NonEmptyString100 value -> value

    override this.ToString() =
        match this with
        | NonEmptyString100 value -> value

[<RequireQualifiedAccess>]
module NonEmptyString100 =

    let private create element =
        match element with
        | Null -> Error "String is null"
        | WhiteSpaces -> Error "String is empty or has white spaces"
        | HasMoreCharsThan 100 -> Error "String has more than 100 chars"
        | _ -> Ok <| NonEmptyString100 element

    let tryFrom element =
        match create element with
        | Ok value -> Some value
        | Error _ -> None

    let newOf element =
        match create element with
        | Ok value -> value
        | Error message -> raise <| ArgumentException(message)

    let value (NonEmptyString100 value) : string = value

    let toString (element: NonEmptyString100) : string = element.ToString()
