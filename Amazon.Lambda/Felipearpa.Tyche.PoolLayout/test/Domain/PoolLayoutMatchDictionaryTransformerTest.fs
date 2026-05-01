module PoolLayoutMatchDictionaryTransformerTest

open Amazon.DynamoDBv2.Model
open FsUnit.Xunit
open Xunit
open Felipearpa.Tyche.PoolLayout.Domain.PoolLayoutMatchDictionaryTransformer

let private baseDictionary () =
    dict
        [ "matchId", AttributeValue(S = "01K0PY8TF2CH2XV22DNN5JE1V5")
          "poolLayoutId", AttributeValue(S = "01K0PY8TF2CH2XV22DNN5JE1V6")
          "homeTeamId", AttributeValue(S = "01K0PY8TF2CH2XV22DNN5JE1V7")
          "homeTeamName", AttributeValue(S = "Tigre")
          "awayTeamId", AttributeValue(S = "01K0PY8TF2CH2XV22DNN5JE1V8")
          "awayTeamName", AttributeValue(S = "América")
          "matchDateTime", AttributeValue(S = "2026-05-01T00:00:00.000Z")
          "poolLayoutVersion", AttributeValue(N = "2") ]

[<Fact>]
let ``given a match with home and away scores when transformed then scores are Some`` () =
    let dictionary =
        System.Collections.Generic.Dictionary(baseDictionary ())

    dictionary["homeTeamScore"] <- AttributeValue(N = "3")
    dictionary["awayTeamScore"] <- AttributeValue(N = "1")

    let model = dictionary.ToPoolLayoutMatch()

    model.HomeTeamScore |> should equal (Some 3)
    model.AwayTeamScore |> should equal (Some 1)

[<Fact>]
let ``given a match without scores when transformed then scores are None`` () =
    let dictionary = baseDictionary ()

    let model = dictionary.ToPoolLayoutMatch()

    model.HomeTeamScore |> should equal None
    model.AwayTeamScore |> should equal None

[<Fact>]
let ``given a match with only home score when transformed then only home is Some`` () =
    let dictionary =
        System.Collections.Generic.Dictionary(baseDictionary ())

    dictionary["homeTeamScore"] <- AttributeValue(N = "2")

    let model = dictionary.ToPoolLayoutMatch()

    model.HomeTeamScore |> should equal (Some 2)
    model.AwayTeamScore |> should equal None
