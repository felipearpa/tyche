/// Reader and writer combinations of the single-release rollout in docs/backend-pagination.md:
/// the original serializer, which wrote and read only `pk` and `sk`, and the released
/// `DynamoDbKeySerializer`, which writes version 1 tokens and reads both formats.
module RolloutCompatibilityTest

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Core
open Felipearpa.Core.Json
open Felipearpa.Data.DynamoDb
open FsUnitTyped
open Xunit

/// The serializer before this change, copied from the last commit for these tests: it writes
/// and reads only the string `pk` and `sk`.
type private OriginalKeySerializer(serializer: ISerializer) =

    interface IKeySerializer with

        member this.Serialize dictionary =
            serializer.Serialize(
                {| Pk = dictionary[Key.pk].S
                   Sk = dictionary[Key.sk].S |}
            )

        member this.Deserialize value =
            let dictionary = serializer.Deserialize<IDictionary<string, obj>>(value)

            dict
                [ Key.pk, AttributeValue(dictionary[Key.pk].ToString())
                  Key.sk, AttributeValue(dictionary[Key.sk].ToString()) ]

let private original = OriginalKeySerializer(JsonSerializer()) :> IKeySerializer
let private released = DynamoDbKeySerializer() :> IKeySerializer

/// A leaderboard page boundary: table keys plus the numeric index key.
let private indexKey () : IDictionary<string, AttributeValue> =
    dict
        [ "pk", AttributeValue(S = "POOL#01K1PX1TX2NM1HG851S1V0P001")
          "sk", AttributeValue(S = "GAMBLER#01K1PX1TX2NM1HG851S1V0GA01")
          "score", AttributeValue(N = "120") ]

let private indexContract: StartKeyContract =
    Map
        [ "pk", StringEqualTo "POOL#01K1PX1TX2NM1HG851S1V0P001"
          "sk", StringStartingWith "GAMBLER#"
          "score", AnyNumber ]

/// A layout-match page boundary: the base table's complete key.
let private baseTableKey () : IDictionary<string, AttributeValue> =
    dict
        [ "pk", AttributeValue(S = "POOLLAYOUT#01K1PX1TX2NM1HG851S1V0Y001")
          "sk", AttributeValue(S = "MATCH#01K1PX1TX2NM1HG851S1V0M001") ]

let private baseTableContract: StartKeyContract =
    Map [ "pk", StringEqualTo "POOLLAYOUT#01K1PX1TX2NM1HG851S1V0Y001"; "sk", StringStartingWith "MATCH#" ]

let private described (key: IDictionary<string, AttributeValue>) =
    key |> Seq.map (fun entry -> entry.Key, entry.Value.S, entry.Value.N) |> Seq.sort |> List.ofSeq

let private startKey contract (reader: IKeySerializer) token =
    try
        Ok(StartKeyContract.startKey contract reader.Deserialize token :> IDictionary<_, _>)
    with InvalidContinuationException reason ->
        Error reason

[<Fact>]
let ``the released serializer writes version 1 tokens that it reads back in full`` () =
    let token = released.Serialize(indexKey ())

    token.StartsWith """{"v":1,""" |> shouldEqual true

    startKey indexContract released token
    |> Result.map described
    |> shouldEqual (Ok(described (indexKey ())))

[<Fact>]
let ``a base-table token from the original serializer stays usable`` () =
    let token = original.Serialize(baseTableKey ())

    startKey baseTableContract released token
    |> Result.map described
    |> shouldEqual (Ok(described (baseTableKey ())))

[<Fact>]
let ``an index token from the original serializer requires a refresh`` () =
    let token = original.Serialize(indexKey ())

    startKey indexContract released token |> shouldEqual (Error(MissingKeyAttribute "score"))

[<Fact>]
let ``the original reader cannot read the released serializer's tokens`` () =
    let token = released.Serialize(indexKey ())

    // A raw lookup failure, not INVALID_CURSOR: the hosts answered it with a server error and
    // fan-out failed. No instance may run the original reader once version 1 tokens exist.
    Assert.Throws<KeyNotFoundException>(fun () -> original.Deserialize token |> ignore) |> ignore
