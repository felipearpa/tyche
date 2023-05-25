namespace Felipearpa.User.Type

open System
open Felipearpa.Type.Validation

[<AutoOpen>]
module private PasswordHelper =

    // * The password must contain at least one digit ((?=.*\d)).
    // * The password must contain at least one lowercase letter ((?=.*[a-z])).
    // * The password must contain at least one uppercase letter ((?=.*[A-Z])).
    // * The password must contain at least one non-word character (i.e. a special character) ((?=.*[^\w\d\s:])).
    // * The password must be between 8 and 16 characters in length (([^\s]){8,16}).
    [<Literal>]
    let passwordRegularExpression =
        @"^(?=.*\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[^\w\d\s:])([^\s]){8,16}$"

type Password =
    private
    | Password of string

    member this.Value =
        match this with
        | Password value -> value

    override this.ToString() =
        match this with
        | Password value -> value

[<RequireQualifiedAccess>]
module Password =

    let private create element =
        match element with
        | Null -> Error "String must be not null"
        | NotMatches passwordRegularExpression -> Error "String must be a valid password"
        | _ -> Ok <| Password element

    let tryOf element =
        match create element with
        | Ok value -> Some value
        | Error _ -> None

    let newOf element =
        match create element with
        | Ok value -> value
        | Error message -> raise <| ArgumentException(message)

    let value (element: Password) = element.Value

    let toString (element: Password) = element.ToString()
