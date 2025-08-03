namespace Felipearpa.Type

open System.Text.RegularExpressions

module Validation =

    let ifTrueThen x =
        function
        | true -> Some x
        | false -> None

    let (|Null|_|) value = value |> isNull |> ifTrueThen Null

    let (|WhiteSpaces|_|) (value: string) =
        value.Trim() = "" |> ifTrueThen WhiteSpaces

    let (|NotMatches|_|) (pattern: string) (value: string) =
        Regex.IsMatch(value, pattern)
        |> not
        |> ifTrueThen NotMatches

    let (|HasMoreCharsThan|_|) length (value: string) =
        value.Length > length
        |> ifTrueThen HasMoreCharsThan

    let (|NotRange|_|) lower upper value =
        (value >= lower && value <= upper)
        |> not
        |> ifTrueThen NotRange

    let (|Less|_|) limit value = value < limit |> ifTrueThen Less

    let (|LessOrEqual|_|) limit value =
        value <= limit |> ifTrueThen LessOrEqual

    let (|Greater|_|) limit value = value > limit |> ifTrueThen Greater

    let (|GreaterOrEqual|_|) limit value =
        value >= limit |> ifTrueThen GreaterOrEqual

    let (|Equal|_|) limit value = value = limit |> ifTrueThen Equal
