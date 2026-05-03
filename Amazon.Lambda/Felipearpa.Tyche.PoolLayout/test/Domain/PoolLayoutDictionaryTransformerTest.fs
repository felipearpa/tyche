module PoolLayoutDictionaryTransformerTest

open System
open Amazon.DynamoDBv2.Model
open FsUnit.Xunit
open Xunit
open Felipearpa.Tyche.PoolLayout.Domain.PoolLayoutDictionaryTransformer

[<Fact>]
let ``given a dictionary when is transformed to object then is transformed correctly`` () =
    let id = "01K0PY8TF2CH2XV22DNN5JE1V5"
    let name = "World Cup 2026"
    let startDateTime = "2026-06-06T06:06:00.000Z"
    let version = 3

    let dictionary =
        dict
            [ "poolLayoutId", AttributeValue(S = id)
              "poolLayoutName", AttributeValue(S = name)
              "startDateTime", AttributeValue(S = startDateTime)
              "poolLayoutVersion", AttributeValue(N = version.ToString()) ]

    let model = dictionary.ToPoolLayout()

    model.Id.Value |> should equal id
    model.Name.Value |> should equal "World Cup 2026"
    model.Version |> should equal version

    model.StartDateTime.ToUniversalTime()
    |> should equal (DateTime.Parse(startDateTime, null, Globalization.DateTimeStyles.AdjustToUniversal))

[<Fact>]
let ``given a dictionary without poolLayoutVersion when is transformed then version defaults to 1`` () =
    let dictionary =
        dict
            [ "poolLayoutId", AttributeValue(S = "01K0PY8TF2CH2XV22DNN5JE1V5")
              "poolLayoutName", AttributeValue(S = "World Cup 2026")
              "startDateTime", AttributeValue(S = "2026-06-06T06:06:00.000Z") ]

    let model = dictionary.ToPoolLayout()

    model.Version |> should equal 1
