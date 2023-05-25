namespace Felipearpa.Type

open System
open Felipearpa.Type.Validation

[<Struct>]
type NonEmptyString =
    private
    | NonEmptyString of string

    override this.ToString() =
        match this with
        | NonEmptyString value -> value

[<RequireQualifiedAccess>]
module NonEmptyString =

    let private create element =
        match element with
        | Null -> Error "String is null"
        | WhiteSpaces -> Error "String is empty or has white spaces"
        | _ -> Ok <| NonEmptyString element

    let tryOf element =
        match create element with
        | Ok value -> Some value
        | Error _ -> None

    let newOf element =
        match create element with
        | Ok value -> value
        | Error message -> raise <| ArgumentException(message)

    let value (NonEmptyString element) : string = element

    let toString (element: NonEmptyString) : string = element.ToString()
