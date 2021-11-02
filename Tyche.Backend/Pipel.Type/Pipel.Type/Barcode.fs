namespace Pipel.Type

open System
open System.Linq
open Validation

[<AutoOpen>]
module private BarcodeHelper =

    [<Literal>]
    let regularExpressionForBarcode = @"^(\d{8}|\d{12,14})$"

    let (|NotChekSum|_|) (value: string) =
        let paddedCode = value.PadLeft(14, '0')

        let sum =
            paddedCode
                .Select(fun c i ->
                    Int32.Parse(c.ToString())
                    * (match i % 2 = 0 with
                       | true -> 3
                       | false -> 1))
                .Sum()

        (sum % 10) = 0 |> not |> ifTrueThen NotChekSum

type Barcode =
    private
    | Barcode of string

    static member Create element =
        match element with
        | Null -> Error "String is null"
        | NotMatches regularExpressionForBarcode -> Error "String is invalid format"
        | NotChekSum -> Error "Checksum is invalid"
        | _ -> Ok <| Barcode element

    static member TryFrom element =
        match Barcode.Create element with
        | Ok it -> Some it
        | Error _ -> None

    static member From element =
        match Barcode.Create element with
        | Ok it -> it
        | Error message -> raise <| ArgumentException(message)

    member this.Value =
        match this with
        | Barcode it -> it

[<RequireQualifiedAccess>]
module Barcode =

    let value (element: Barcode) = element.Value

    let toString (element: Barcode) = TypeBuilder.toString element
