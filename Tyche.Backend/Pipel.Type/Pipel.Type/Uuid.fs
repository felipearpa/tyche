namespace Pipel.Type

open System
open Validation

[<AutoOpen>]
module private UuidHelper =

    [<Literal>]
    let regularExpressionForUuid =
        @"^[0-9a-fA-F]{8}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{12}$"

type Uuid =
    private
    | Uuid of Guid

    static member Create element =
        match element with
        | Null -> Error "String is null"
        | NotMatches regularExpressionForUuid -> Error "String has an invalid format"
        | _ ->
            match Guid.TryParse element with
            | true, i -> Ok <| Uuid i
            | _ -> Error "String has an invalid format"

    static member TryFrom element =
        match Uuid.Create element with
        | Ok it -> Some it
        | Error _ -> None

    static member From element =
        match Uuid.Create element with
        | Ok it -> it
        | Error message -> raise <| ArgumentException(message)

    static member From element = Uuid element

    static member TryFromNullable(element: Guid Nullable) =
        match element.HasValue with
        | false -> None
        | true -> Some <| Uuid.From element.Value

    static member FromNullable element =
        match Uuid.TryFromNullable element with
        | Some it -> it
        | None -> raise <| ArgumentException("Uuid is null")

    member this.Value =
        match this with
        | Uuid it -> it

[<RequireQualifiedAccess>]
module Uuid =

    let value (element: Uuid) = element.Value

    let newUuid () = Uuid(Guid.NewGuid())

    let toString (element: Uuid) = TypeBuilder.toString element
