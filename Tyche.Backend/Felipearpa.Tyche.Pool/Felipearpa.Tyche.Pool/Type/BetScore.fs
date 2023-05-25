namespace Felipearpa.Tyche.Pool.Type

open System
open Felipearpa.Type.Validation

[<Struct>]
type BetScore =
    private
    | BetScore of int

    override this.ToString() =
        match this with
        | BetScore value -> value.ToString()

[<RequireQualifiedAccess>]
module BetScore =

    let private create element =
        match element with
        | NotRange 0 999 -> Error "Value is out of range [0, 999]"
        | _ -> Ok <| BetScore element

    let tryOf element =
        match create element with
        | Ok it -> Some it
        | Error _ -> None

    let newOf element =
        match create element with
        | Ok it -> it
        | Error message -> raise <| ArgumentException(message)

    let value (BetScore element) = element

    let toString (element: BetScore) = element.ToString()
