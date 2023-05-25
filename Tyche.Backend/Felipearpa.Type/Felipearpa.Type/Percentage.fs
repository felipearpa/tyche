namespace Felipearpa.Type

open System
open Felipearpa.Type.Validation

[<Struct>]
type Percentage =
    private
    | Percentage of float

    override this.ToString() =
        match this with
        | Percentage value -> value.ToString()

[<RequireQualifiedAccess>]
module Percentage =

    let private create element =
        match element with
        | NotRange 0.0 1.0 -> Error "Value is out of range [0, 1]"
        | _ -> Ok <| Percentage element

    let tryOf element =
        match create element with
        | Ok it -> Some it
        | Error _ -> None

    let newOf element =
        match create element with
        | Ok it -> it
        | Error message -> raise <| ArgumentException(message)

    let value (Percentage element) = element

    let toString (element: Percentage) = element.ToString()
