module KeySerializerTests

open System
open System.Collections.Generic
open System.IO
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb
open FsUnitTyped
open Xunit

let private s text = AttributeValue(S = text)
let private n number = AttributeValue(N = number)
let private b (bytes: byte array) = AttributeValue(B = new MemoryStream(bytes))

/// The type letter and value text of an attribute, failing when it holds anything but one scalar.
let private describe (attribute: AttributeValue) =
    match attribute.S, attribute.N, attribute.B with
    | text, null, null when not (isNull text) -> "S", text
    | null, number, null when not (isNull number) -> "N", number
    | null, null, bytes when not (isNull bytes) -> "B", Convert.ToBase64String(bytes.ToArray())
    | _ -> failwith "the attribute is not exactly one scalar"

let private describeAll (key: IDictionary<string, AttributeValue>) =
    key |> Seq.map (fun entry -> entry.Key, describe entry.Value) |> Map.ofSeq

let private roundTrip (key: IDictionary<string, AttributeValue>) =
    let token =
        ContinuationKey.ofLastEvaluatedKey key
        |> Option.defaultWith (fun () -> failwith "expected a continuation key")
        |> ContinuationToken.encode

    match ContinuationToken.decode token with
    | Ok decoded -> ContinuationKey.toAttributeValues decoded :> IDictionary<_, _>
    | Error error -> failwith $"%A{error}"

let private decodeError token =
    match ContinuationToken.decode token with
    | Ok key -> failwith $"expected an error, decoded %A{ContinuationKey.attributes key}"
    | Error error -> error

let private keySerializer = DynamoDbKeySerializer() :> IKeySerializer

let versionedKeys: obj array seq =
    [ "leaderboard: string table keys and a numeric score",
      dict [ "pk", s "POOL#01KQ7PXAR21QBJBEEQ7EFRNQGE"; "sk", s "GAMBLER#01KQ7PWR1HGFE4TXT5MPSZW595"; "score", n "120" ]
      "pending bets: a date index key",
      dict [ "pk", s "GAMBLER#01A#POOL#01B"; "sk", s "MATCH#01C"; "matchDateTime", s "2026-06-12T17:00:00Z" ]
      "fan-out: composite index keys",
      dict
          [ "pk", s "POOL#01B"
            "sk", s "GAMBLER#01A"
            "getGamblersByPoolLayoutPk", s "POOLLAYOUT#01L"
            "getGamblersByPoolLayoutSk", s "POOL#01B#GAMBLER#01A" ]
      "open layouts: status and start date",
      dict [ "pk", s "POOLLAYOUT#01L"; "sk", s "POOLLAYOUT#01L"; "status", s "OPENED"; "startDateTime", s "2026-06-11T00:00:00Z" ]
      "member names: Unicode, quotes, backslashes, and URL-significant characters",
      dict [ "pk", s "POOL#01B"; "sk", s "GAMBLER#01A"; "gamblerUsername", s "Zoë \"el 10\" \\ 名前 🎉 +&=/?#%" ]
      "numbers: zero",
      dict [ "pk", s "POOL#01B"; "sk", s "GAMBLER#01A"; "score", n "0" ]
      "numbers: negative",
      dict [ "pk", s "POOL#01B"; "sk", s "GAMBLER#01A"; "score", n "-7" ]
      "numbers: 38 significant digits",
      dict [ "pk", s "POOL#01B"; "sk", s "GAMBLER#01A"; "score", n "12345678901234567890.123456789012345678" ]
      "numbers: negative fraction and exponent",
      dict [ "pk", s "POOL#01B"; "sk", s "GAMBLER#01A"; "small", n "-0.000000000000000000000000000001"; "large", n "1E+125" ]
      "binary bytes, including zero and 0xFF",
      dict [ "pk", s "POOL#01B"; "sk", s "GAMBLER#01A"; "blob", b [| 0uy; 1uy; 127uy; 128uy; 255uy; 0uy |] ] ]
    |> List.map (fun (name, key) -> [| box name; box key |])
    |> Seq.ofList

[<Theory>]
[<MemberData(nameof versionedKeys)>]
let ``a version 1 token round trip keeps every attribute name, type, and value`` (_: string, key: IDictionary<string, AttributeValue>) =
    roundTrip key |> describeAll |> shouldEqual (describeAll key)

[<Fact>]
let ``a version 1 token has the documented wire format`` () =
    dict [ "pk", s "POOL#example"; "sk", s "GAMBLER#example"; "score", n "120" ]
    |> ContinuationKey.ofLastEvaluatedKey
    |> Option.map ContinuationToken.encode
    |> shouldEqual (Some """{"v":1,"key":{"pk":{"S":"POOL#example"},"score":{"N":"120"},"sk":{"S":"GAMBLER#example"}}}""")

[<Fact>]
let ``the documented version 1 examples decode to typed attributes in any member order`` () =
    let expected =
        Map
            [ "pk", ("S", "POOL#example")
              "sk", ("S", "GAMBLER#example")
              "score", ("N", "120")
              "blob", ("B", "AAH/") ]

    [ """{"v":1,"key":{"pk":{"S":"POOL#example"},"sk":{"S":"GAMBLER#example"},"score":{"N":"120"},"blob":{"B":"AAH/"}}}"""
      """{"key":{"blob":{"B":"AAH/"},"score":{"N":"120"},"sk":{"S":"GAMBLER#example"},"pk":{"S":"POOL#example"}},"v":1}""" ]
    |> List.iter (fun token -> keySerializer.Deserialize token |> describeAll |> shouldEqual expected)

[<Fact>]
let ``a number keeps its exact text instead of a normalized value`` () =
    dict [ "pk", s "POOL#01B"; "sk", s "GAMBLER#01A"; "score", n "120.50" ]
    |> roundTrip
    |> fun key -> key["score"].N
    |> shouldEqual "120.50"

[<Fact>]
let ``a legacy token decodes as string table keys`` () =
    keySerializer.Deserialize """{"pk":"POOLLAYOUT#01L","sk":"MATCH#01M"}"""
    |> describeAll
    |> shouldEqual (Map [ "pk", ("S", "POOLLAYOUT#01L"); "sk", ("S", "MATCH#01M") ])

[<Fact>]
let ``the key serializer writes version 1 tokens with every key attribute`` () =
    dict [ "pk", s "POOL#example"; "sk", s "GAMBLER#example"; "score", n "120" ]
    |> keySerializer.Serialize
    |> shouldEqual """{"v":1,"key":{"pk":{"S":"POOL#example"},"score":{"N":"120"},"sk":{"S":"GAMBLER#example"}}}"""

[<Theory>]
[<MemberData(nameof versionedKeys)>]
let ``a key round trips through the key serializer`` (_: string, key: IDictionary<string, AttributeValue>) =
    key |> keySerializer.Serialize |> keySerializer.Deserialize |> describeAll |> shouldEqual (describeAll key)

let malformedTokens: obj array seq =
    [ ""
      "   "
      "not json"
      "[]"
      "\"text\""
      "{}"
      """{"v":"1","key":{"pk":{"S":"a"}}}"""
      """{"v":1.5,"key":{"pk":{"S":"a"}}}"""
      """{"v":1}"""
      """{"key":{"pk":{"S":"a"}}}"""
      """{"v":1,"key":[]}"""
      """{"v":1,"key":{}}"""
      """{"v":1,"key":{"pk":{"S":"a"}},"extra":true}"""
      """{"v":1,"key":{"pk":"a"}}"""
      """{"v":1,"key":{"pk":{}}}"""
      """{"v":1,"key":{"pk":{"S":"a","N":"1"}}}"""
      """{"v":1,"key":{"pk":{"S":"a"},"pk":{"S":"b"}}}"""
      """{"v":1,"v":1,"key":{"pk":{"S":"a"}}}"""
      """{"pk":"a"}"""
      """{"pk":"a","sk":"b","score":"1"}"""
      """{"pk":"a","sk":"b","pk":"c"}"""
      """{"v":1,"key":{"pk":{"S":"a"}}""" ]
    |> List.map (fun token -> [| box token |])
    |> Seq.ofList

[<Theory>]
[<MemberData(nameof malformedTokens)>]
let ``a token with a broken envelope is malformed`` (token: string) =
    match decodeError token with
    | MalformedToken _ -> ()
    | other -> failwith $"expected MalformedToken, got %A{other}"

[<Fact>]
let ``a null token is malformed`` () =
    match decodeError null with
    | MalformedToken _ -> ()
    | other -> failwith $"expected MalformedToken, got %A{other}"

[<Theory>]
[<InlineData(0)>]
[<InlineData(2)>]
[<InlineData(-1)>]
let ``a token with another version is unsupported`` (version: int) =
    decodeError ("""{"v":""" + string version + ""","key":{"pk":{"S":"a"}}}""")
    |> shouldEqual (UnsupportedVersion version)

[<Theory>]
[<InlineData("SS", """["a"]""")>]
[<InlineData("NS", """["1"]""")>]
[<InlineData("BS", """["AA=="]""")>]
[<InlineData("M", """{}""")>]
[<InlineData("L", """[]""")>]
[<InlineData("BOOL", "true")>]
[<InlineData("NULL", "true")>]
[<InlineData("s", "\"a\"")>]
let ``a key attribute with a non-scalar type is unsupported`` (typeName: string, value: string) =
    decodeError (sprintf "{\"v\":1,\"key\":{\"pk\":{\"S\":\"a\"},\"score\":{\"%s\":%s}}}" typeName value)
    |> shouldEqual (UnsupportedAttributeType("score", typeName))

[<Theory>]
[<InlineData("""{"N":120}""")>]
[<InlineData("""{"N":"abc"}""")>]
[<InlineData("""{"N":""}""")>]
[<InlineData("""{"N":"1e"}""")>]
[<InlineData("""{"N":"1."}""")>]
[<InlineData("""{"N":"+1"}""")>]
[<InlineData("""{"N":" 1"}""")>]
[<InlineData("""{"N":"NaN"}""")>]
[<InlineData("""{"N":"Infinity"}""")>]
[<InlineData("""{"N":"0x10"}""")>]
[<InlineData("""{"N":"123456789012345678901234567890123456789"}""")>]
[<InlineData("""{"S":""}""")>]
[<InlineData("""{"S":1}""")>]
[<InlineData("""{"S":null}""")>]
[<InlineData("""{"S":"\ud800"}""")>]
[<InlineData("""{"B":"not base64!"}""")>]
[<InlineData("""{"B":""}""")>]
[<InlineData("""{"B":true}""")>]
let ``a key attribute with an invalid value is rejected`` (attribute: string) =
    match decodeError ("""{"v":1,"key":{"pk":{"S":"a"},"score":""" + attribute + "}}") with
    | InvalidAttributeValue("score", _) -> ()
    | other -> failwith $"expected InvalidAttributeValue for score, got %A{other}"

[<Theory>]
[<InlineData("""{"pk":"","sk":"b"}""")>]
[<InlineData("""{"pk":"a","sk":1}""")>]
[<InlineData("""{"pk":"a","sk":null}""")>]
let ``a legacy token with an invalid value is rejected`` (token: string) =
    match decodeError token with
    | InvalidAttributeValue _ -> ()
    | other -> failwith $"expected InvalidAttributeValue, got %A{other}"

[<Fact>]
let ``the key serializer raises a typed failure for an invalid token`` () =
    let error =
        Assert.Throws<InvalidContinuationException>(fun () -> keySerializer.Deserialize """{"v":2,"key":{}}""" |> ignore)

    error.Data0 |> shouldEqual (UnsupportedVersion 2)

[<Fact>]
let ``an absent last evaluated key has no next token`` () =
    KeySerializer.next keySerializer null |> shouldEqual None
    ContinuationKey.ofLastEvaluatedKey null |> shouldEqual None

[<Fact>]
let ``an empty last evaluated key has no next token`` () =
    let empty = Dictionary<string, AttributeValue>()
    KeySerializer.next keySerializer empty |> shouldEqual None
    ContinuationKey.ofLastEvaluatedKey empty |> shouldEqual None

[<Fact>]
let ``a nonempty last evaluated key has a next token`` () =
    dict [ "pk", s "POOLLAYOUT#01L"; "sk", s "MATCH#01M" ]
    |> KeySerializer.next keySerializer
    |> shouldEqual (Some """{"v":1,"key":{"pk":{"S":"POOLLAYOUT#01L"},"sk":{"S":"MATCH#01M"}}}""")

[<Fact>]
let ``serializing an empty key fails instead of writing an empty token`` () =
    Assert.Throws<ArgumentException>(fun () -> keySerializer.Serialize(Dictionary()) |> ignore)
    |> ignore

[<Fact>]
let ``a last evaluated key with a non-scalar attribute is invalid service state`` () =
    Assert.Throws<InvalidOperationException>(fun () ->
        dict [ "pk", s "POOL#01B"; "flag", AttributeValue(BOOL = true) ]
        |> ContinuationKey.ofLastEvaluatedKey
        |> ignore)
    |> ignore

[<Fact>]
let ``decoded binary values are independent copies`` () =
    let bytes = [| 1uy; 2uy; 3uy |]
    let key = dict [ "pk", s "POOL#01B"; "blob", b bytes ] |> ContinuationKey.ofLastEvaluatedKey |> Option.get
    let first = ContinuationKey.toAttributeValues key
    first["blob"].B.WriteByte(9uy)
    bytes[0] <- 7uy

    let second = ContinuationKey.toAttributeValues key
    second["blob"].B.ToArray() |> shouldEqual [| 1uy; 2uy; 3uy |]
