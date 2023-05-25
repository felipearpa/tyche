namespace Felipearpa.Type

open System
open Felipearpa.Type.Validation

[<AutoOpen>]
module private EmailHelper =

    [<Literal>]
    let regularExpressionForEmail = @"^([^.@]+)(\.[^.@]+)*@([^.@]+\.)+([^.@]+)$"

[<Struct>]
type Email =
    private
    | Email of string

    override this.ToString() =
        match this with
        | Email value -> value

[<RequireQualifiedAccess>]
module Email =

    let private create element =
        match element with
        | Null -> Error "String is null"
        | NotMatches regularExpressionForEmail -> Error "String is invalid format"
        | _ -> Ok <| Email element

    let tryOf element =
        match create element with
        | Ok value -> Some value
        | Error _ -> None

    let newOf element =
        match create element with
        | Ok value -> value
        | Error message -> raise <| ArgumentException(message)

    let value (Email element) = element

    let toString (element: Email) = element.ToString()
