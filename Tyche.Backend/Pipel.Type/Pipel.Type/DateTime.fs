namespace Pipel.Type

open System
open Validation

[<AutoOpen>]
module private DateTimeHelper =

    [<Literal>]
    let regularExpressionForDateTime =
        @"^(?:[1-9]\d{3}-(?:(?:0[1-9]|1[0-2])-(?:0[1-9]|1\d|2[0-8])|(?:0[13-9]|1[0-2])-(?:29|30)|(?:0[13578]|1[02])-31)|(?:[1-9]\d(?:0[48]|[2468][048]|[13579][26])|(?:[2468][048]|[13579][26])00)-02-29)T(?:[01]\d|2[0-3]):[0-5]\d:[0-5]\d(?:\.\d{1,9})?(?:Z|[+-][01]\d:[0-5]\d)$"

[<CustomEquality>]
[<CustomComparison>]
type DateTime =
    private
    | DateTime of System.DateTime

    static member Create element =
        match element with
        | Null -> Error "Value is null"
        | NotMatches regularExpressionForDateTime -> Error "Values has an invalid format"
        | _ ->
            match DateTime.TryParse(element) with
            | true, d -> Ok <| DateTime d
            | _ -> Error "Values has an invalid format"

    static member TryFrom element =
        match DateTime.Create element with
        | Ok it -> Some it
        | Error _ -> None

    static member From element =
        match DateTime.Create element with
        | Ok it -> it
        | Error message -> raise <| ArgumentException(message)

    static member From element = DateTime element

    member this.Value =
        match this with
        | DateTime it -> it

    override this.GetHashCode() =
        match this with
        | DateTime it -> it.GetHashCode()

    override this.Equals(obj) =
        match (this, obj :?> DateTime) with
        | DateTime a, DateTime b -> a.Equals b

    interface IComparable with

        member this.CompareTo(obj) =
            match (this, obj :?> DateTime) with
            | DateTime a, DateTime b -> a.CompareTo b

module DateTime =

    let inline tryFrom (element: System.DateTime) : ^Type option = TypeBuilder.tryFrom element

    let inline from (element: System.DateTime) = TypeBuilder.from element

    let inline value (element: ^Type) : System.DateTime = TypeBuilder.value element

    let now () = DateTime.From System.DateTime.Now

    let inline toString (element: ^Type) = TypeBuilder.toString element

    let inline toStringWithFormat (format: string) (element: ^Type) =
        let dateTime: System.DateTime = element |> value
        dateTime.ToString(format)

    let inline toUniversalTime (element: ^Type) : ^Type =
        let dateTime: System.DateTime = element |> value
        dateTime.ToUniversalTime() |> TypeBuilder.from

    let inline addDays daysCount (element: ^Type) : ^Type =
        let dateTime: System.DateTime = element |> value
        dateTime.AddDays daysCount |> TypeBuilder.from
