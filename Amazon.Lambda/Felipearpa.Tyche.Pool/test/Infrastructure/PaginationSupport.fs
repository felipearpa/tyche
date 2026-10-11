namespace Felipearpa.Tyche.Pool.Test.Infrastructure

open System.Collections.Generic
open System.Text.Json
open Amazon.DynamoDBv2.Model
open Felipearpa.Core.Paging
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.Pool.Test.Fakes

/// Writes the `pk`/`sk` tokens that the backend issued before version 1, as a client may still
/// hold them, and reads tokens with the production `DynamoDbKeySerializer`.
type LegacyTokenKeySerializer() =
    let reader = DynamoDbKeySerializer() :> IKeySerializer

    interface IKeySerializer with
        member _.Serialize lastEvaluatedKey =
            JsonSerializer.Serialize
                {| pk = lastEvaluatedKey[Key.pk].S
                   sk = lastEvaluatedKey[Key.sk].S |}

        member _.Deserialize token = reader.Deserialize token

module PaginationSupport =

    /// The production serializer, which emits version 1 tokens.
    let typed = DynamoDbKeySerializer() :> IKeySerializer

    /// Emits legacy `pk`/`sk` tokens held from the earlier backend.
    let legacyTokens = LegacyTokenKeySerializer() :> IKeySerializer

    let s (value: string) = AttributeValue(S = value)
    let n (value: int) = AttributeValue(N = string value)

    /// Each attribute's scalar type letter and value text.
    let describeKey (key: IDictionary<string, AttributeValue>) : Map<string, string * string> =
        key
        |> Seq.map (fun entry ->
            match entry.Value.S, entry.Value.N with
            | text, null when not (isNull text) -> entry.Key, ("S", text)
            | null, number when not (isNull number) -> entry.Key, ("N", number)
            | _ -> failwith $"attribute {entry.Key} is not a string or number")
        |> Map.ofSeq

    /// The described start key of every query received since the last call; `None` for a
    /// query without one.
    let takeStartKeys (fake: FakeDynamoDb) =
        fake.TakeQueries()
        |> List.map (fun query ->
            query.ExclusiveStartKey
            |> Option.ofObj
            |> Option.filter (fun key -> key.Count > 0)
            |> Option.map describeKey)

    /// Follows `next` from the first page until it is absent and returns every page. Fails
    /// when the traversal does not end within 50 pages.
    let traverse (fetch: string option -> Async<CursorPage<'T>>) : Async<('T list * string option) list> =
        let rec loop next pages =
            async {
                if List.length pages >= 50 then
                    failwith "the traversal did not end"

                let! page = fetch next
                let pages = pages @ [ List.ofSeq page.Items, page.Next ]

                match page.Next with
                | Some _ -> return! loop page.Next pages
                | None -> return pages
            }

        loop None []

    /// The failure of `work`, which must fail.
    let failureOf (work: Async<'T>) =
        async {
            match! work |> Async.Catch with
            | Choice1Of2 _ -> return failwith "expected the call to fail"
            | Choice2Of2 error -> return error
        }

    /// The invalid-continuation reason `work` fails with.
    let invalidContinuationOf (work: Async<'T>) =
        async {
            match! failureOf work with
            | InvalidContinuationException reason -> return reason
            | other -> return failwith $"expected an invalid continuation, got {other}"
        }

    /// A version 1 token for the given key attributes.
    let tokenOf (attributes: (string * AttributeValue) list) =
        typed.Serialize(Dictionary(dict attributes))
