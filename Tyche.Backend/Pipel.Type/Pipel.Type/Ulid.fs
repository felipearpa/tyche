namespace Pipel.Type

open System
open Pipel.Type
open Validation

[<AutoOpen>]
module private UlidHelper =

    [<Literal>]
    let regularExpressionForULID =
        @"^[0123456789ABCDEFGHJKMNPQRSTVWXYZ]{26}$"

type Ulid =
    private
    | Ulid of System.Ulid

    static member Create element =
        match element with
        | Null -> Error "String must be not null"
        | NotMatches regularExpressionForULID -> Error "String must be an ULID format"
        | _ ->
            match Ulid.TryParse element with
            | true, it -> Ok <| Ulid it
            | _ -> Error "String has an invalid format"

    static member TryFrom element =
        match Ulid.Create element with
        | Ok it -> Some it
        | Error _ -> None

    static member From element =
        match Ulid.Create element with
        | Ok it -> it
        | Error message -> raise <| ArgumentException(message)

    static member From element = Ulid element

    member this.Value =
        match this with
        | Ulid it -> it

[<RequireQualifiedAccess>]
module Ulid =

    let value (element: Ulid) = element.Value
    
    let newUlid () = System.Ulid.NewUlid() |> Ulid

    let toString (element: Ulid) = TypeBuilder.toString element
