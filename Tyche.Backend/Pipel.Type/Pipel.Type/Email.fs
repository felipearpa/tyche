namespace Pipel.Type

open System
open Pipel.Type
open Validation

[<AutoOpen>]
module private EmailHelper =

    [<Literal>]
    let regularExpressionForEmail = @"^([^.@]+)(\.[^.@]+)*@([^.@]+\.)+([^.@]+)$"

type Email =
    private
    | Email of string

    static member Create element =
        match element with
        | Null -> Error "String is null"
        | NotMatches regularExpressionForEmail -> Error "String is invalid format"
        | _ -> Ok <| Email element

    static member TryFrom element =
        match Email.Create element with
        | Ok it -> Some it
        | Error _ -> None

    static member From element =
        match Email.Create element with
        | Ok it -> it
        | Error message -> raise <| ArgumentException(message)

    member this.Value =
        match this with
        | Email it -> it

[<RequireQualifiedAccess>]
module Email =

    let value (element: Email) = element.Value

    let toString (element: Email) = TypeBuilder.toString element
