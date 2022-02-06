namespace Pipel.Type

open System
open Validation

type PositiveInt =
    private
    | PositiveInt of int

    static member Create element =
        match element with
        | Less 0 -> Error "It must be greater or equal than zero"
        | _ -> Ok <| PositiveInt element

    static member TryFrom element =
        match PositiveInt.Create element with
        | Ok it -> Some it
        | Error _ -> None

    static member From element =
        match PositiveInt.Create element with
        | Ok it -> it
        | Error message -> raise <| ArgumentException(message)

    static member TryFromNullable(element: int Nullable) =
        match element.HasValue with
        | false -> None
        | true -> Some <| PositiveInt.From element.Value

    static member FromNullable element =
        match PositiveInt.TryFromNullable element with
        | Some it -> it
        | None ->
            raise
            <| ArgumentException("It must be different of null")

    member this.Value =
        match this with
        | PositiveInt it -> it

    static member (+)(a, b) =
        match (a, b) with
        | PositiveInt itx, PositiveInt ity -> PositiveInt.From(itx + ity)

    static member (-)(a, b) =
        match (a, b) with
        | PositiveInt itx, PositiveInt ity -> PositiveInt.From(itx - ity)

    static member (*)(a, b) =
        match (a, b) with
        | PositiveInt itx, PositiveInt ity -> PositiveInt.From(itx * ity)

    static member (/)(a, b) =
        match (a, b) with
        | PositiveInt itx, PositiveInt ity -> PositiveInt.From(itx / ity)

[<RequireQualifiedAccess>]
module PositiveInt =

    let value (element: PositiveInt) = TypeBuilder.value element

    let toString (element: PositiveInt) : string = TypeBuilder.toString element

    let nullableValue (element: PositiveInt option) = TypeBuilder.nullableValue element
