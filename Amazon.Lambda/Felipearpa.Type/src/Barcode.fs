namespace Felipearpa.Type

open System
open System.Linq
open Felipearpa.Type.Validation

[<AutoOpen>]
module private BarcodeHelper =

    [<Literal>]
    let regularExpressionForBarcode = @"^(\d{8}|\d{12,14})$"

    let (|NotCheckSum|_|) (value: string) =
        let paddedCode = value.PadLeft(14, '0')

        let sum =
            paddedCode
                .Select(fun c i ->
                    Int32.Parse(c.ToString())
                    * (match i % 2 = 0 with
                       | true -> 3
                       | false -> 1))
                .Sum()

        (sum % 10) = 0 |> not |> ifTrueThen NotCheckSum

[<Struct>]
type Barcode =
    private
    | Barcode of string

    member this.Value =
        match this with
        | Barcode value -> value

    override this.ToString() =
        match this with
        | Barcode value -> value

[<RequireQualifiedAccess>]
module Barcode =

    let private create element =
        match element with
        | Null -> Error "String is null"
        | NotMatches regularExpressionForBarcode -> Error "String is invalid format"
        | NotCheckSum -> Error "Checksum is invalid"
        | _ -> Ok <| Barcode element

    let tryOf element =
        match create element with
        | Ok value -> Some value
        | Error _ -> None

    let newOf element =
        match create element with
        | Ok value -> value
        | Error message -> raise <| ArgumentException(message)

    let value (Barcode value) = value

    let toString (element: Barcode) = element.ToString()
