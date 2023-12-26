namespace Felipearpa.Account.Type

open System
open Felipearpa.Type.Validation

[<AutoOpen>]
module private UsernameHelper =

    // * The username may only contain letters, numbers, underscores, and hyphens ([a-zA-Z0-9_-]).
    // * The username must be between 3 and 16 characters in length ({3,16}).
    [<Literal>]
    let usernameRegularExpression =
        @"^[a-zA-Z0-9_-]{3,16}$"

type Username =
    private
    | Username of string

    member this.Value =
        match this with
        | Username value -> value

    override this.ToString() =
        match this with
        | Username value -> value

[<RequireQualifiedAccess>]
module Username =

    let private create element =
        match element with
        | Null -> Error "String must be not null"
        | NotMatches usernameRegularExpression -> Error "String must be a valid username"
        | _ -> Ok <| Username element

    let tryOf element =
        match create element with
        | Ok value -> Some value
        | Error _ -> None

    let newOf element =
        match create element with
        | Ok value -> value
        | Error message -> raise <| ArgumentException(message)

    let value (element: Username) = element.Value

    let toString (element: Username) = element.ToString()
