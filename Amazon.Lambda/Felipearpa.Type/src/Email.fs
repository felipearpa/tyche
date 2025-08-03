namespace Felipearpa.Type

open System
open Felipearpa.Type.Validation

[<AutoOpen>]
module private EmailHelper =

    // ^ and $ are the start and end anchors, respectively, to match the entire string from start to end.
    // [A-Za-z0-9._%+-]+ matches one or more of the following characters: uppercase letters, lowercase letters, digits, period (.), underscore (_), percent sign (%), plus sign (+), or hyphen/minus sign (-). This corresponds to the local part of the email address before the @ symbol.
    // @ matches the literal @ symbol.
    // [A-Za-z0-9.-]+ matches one or more of the following characters: uppercase letters, lowercase letters, digits, period (.), or hyphen/minus sign (-). This corresponds to the domain part of the email address after the @ symbol.
    // \. matches the literal . character. It needs to be escaped with a backslash (\) because . has a special meaning in regex.
    // [A-Za-z]{2,} matches two or more uppercase or lowercase letters. This corresponds to the top-level domain (TLD) part of the email address (e.g., com, org, net).
    [<Literal>]
    let regularExpressionForEmail = @"^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$"

[<Struct>]
type Email =
    private
    | Email of string

    member this.Value =
        match this with
        | Email value -> value

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

    let value (Email value) = value

    let toString (element: Email) = element.ToString()
