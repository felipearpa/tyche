module StartKeyContractTest

open System.Collections.Generic
open System.IO
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb
open FsUnitTyped
open Xunit

let private s text = AttributeValue(S = text)
let private n number = AttributeValue(N = number)

let private contract: StartKeyContract =
    Map
        [ "pk", StringEqualTo "PARTITION#1"
          "sk", StringStartingWith "ITEM#"
          "rank", AnyNumber
          "label", AnyString ]

let private key (attributes: (string * AttributeValue) list) =
    Dictionary(dict attributes) :> IDictionary<_, _>

let private complete =
    [ "pk", s "PARTITION#1"; "sk", s "ITEM#7"; "rank", n "0"; "label", s "Zoë" ]

let private replacing name value =
    complete |> List.map (fun (attribute, current) -> attribute, (if attribute = name then value else current))

let private deserializer = DynamoDbKeySerializer() :> IKeySerializer

[<Fact>]
let ``given a key with exactly the contract's attributes when checked then it is accepted`` () =
    StartKeyContract.check contract (key complete) |> shouldEqual (Ok())

let rejectedKeys: obj array seq =
    [ "missing number", complete |> List.filter (fst >> (<>) "rank"), MissingKeyAttribute "rank"
      "extra attribute", complete @ [ "other", s "x" ], UnexpectedKeyAttribute "other"
      "number as string", replacing "rank" (s "5"), WrongKeyAttributeType("rank", "N", "S")
      "string as number", replacing "label" (n "5"), WrongKeyAttributeType("label", "S", "N")
      "binary for a string",
      replacing "label" (AttributeValue(B = new MemoryStream([| 1uy |]))),
      WrongKeyAttributeType("label", "S", "B")
      "another partition",
      replacing "pk" (s "PARTITION#2"),
      KeyOutOfScope("pk", "the value belongs to another partition or key")
      "another prefix", replacing "sk" (s "OTHER#7"), KeyOutOfScope("sk", "the value does not start with 'ITEM#'") ]
    |> Seq.map (fun (case, attributes, reason) -> [| box case; box attributes; box reason |])

[<Theory; MemberData(nameof rejectedKeys)>]
let ``given a key that does not fit the contract when checked then the reason is reported``
    (_case: string)
    (attributes: (string * AttributeValue) list)
    (expected: InvalidContinuation)
    =
    StartKeyContract.check contract (key attributes) |> shouldEqual (Error expected)

[<Fact>]
let ``given a version 1 token that fits when a start key is built then every attribute is kept`` () =
    let token =
        ContinuationKey.ofLastEvaluatedKey (key complete) |> Option.get |> ContinuationToken.encode

    let startKey = StartKeyContract.startKey contract deserializer.Deserialize token

    startKey.Keys |> Seq.sort |> List.ofSeq |> shouldEqual [ "label"; "pk"; "rank"; "sk" ]
    startKey["rank"].N |> shouldEqual "0"
    startKey["label"].S |> shouldEqual "Zoë"

[<Fact>]
let ``given a legacy token for an indexed contract when a start key is built then it raises the missing attribute`` () =
    let legacy = """{"pk":"PARTITION#1","sk":"ITEM#7"}"""

    let error =
        Assert.Throws<InvalidContinuationException>(fun () ->
            StartKeyContract.startKey contract deserializer.Deserialize legacy |> ignore)

    error.Data0 |> shouldEqual (MissingKeyAttribute "label")

[<Fact>]
let ``given a malformed token when a start key is built then the decode failure is raised unchanged`` () =
    let error =
        Assert.Throws<InvalidContinuationException>(fun () ->
            StartKeyContract.startKey contract deserializer.Deserialize "not json" |> ignore)

    error.Data0 |> shouldEqual (MalformedToken "the token is not JSON")
