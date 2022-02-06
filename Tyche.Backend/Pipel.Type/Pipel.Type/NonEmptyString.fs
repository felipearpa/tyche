namespace Pipel.Type

open System
open Validation

type NonEmptyString =
    private
    | NonEmptyString of string

    static member Create element =
        match element with
        | Null -> Error "String is null"
        | WhiteSpaces -> Error "String is empty or has white spaces"
        | _ -> Ok <| NonEmptyString element

    static member TryFrom element =
        match NonEmptyString.Create element with
        | Ok it -> Some it
        | Error _ -> None

    static member From element =
        match NonEmptyString.Create element with
        | Ok it -> it
        | Error message -> raise <| ArgumentException(message)

    member this.Value =
        match this with
        | NonEmptyString it -> it

type NonEmptyString100 =
    private
    | NonEmptyString100 of string

    static member Create element =
        match element with
        | Null -> Error "String is null"
        | WhiteSpaces -> Error "String is empty or has white spaces"
        | HasMoreCharsThan 100 -> Error "String has more than 100 chars"
        | _ -> Ok <| NonEmptyString100 element

    static member TryFrom element =
        match NonEmptyString100.Create element with
        | Ok it -> Some it
        | Error _ -> None

    static member From element =
        match NonEmptyString100.Create element with
        | Ok it -> it
        | Error message -> raise <| ArgumentException(message)

    member this.Value =
        match this with
        | NonEmptyString100 it -> it

[<RequireQualifiedAccess>]
module String =

    let tryFrom (element: string) =
        match isNull element with
        | false -> Some element
        | _ -> None

[<RequireQualifiedAccess>]
module NonEmptyString =

    let value (element: NonEmptyString) : string = TypeBuilder.value element

    let toString (element: NonEmptyString) : string = TypeBuilder.toString element

[<RequireQualifiedAccess>]
module NonEmptyString100 =

    let value (element: NonEmptyString100) : string = TypeBuilder.value element

    let toString (element: NonEmptyString100) : string = TypeBuilder.toString element
