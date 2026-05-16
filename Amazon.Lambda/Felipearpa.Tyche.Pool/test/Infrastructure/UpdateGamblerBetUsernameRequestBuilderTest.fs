module UpdateGamblerBetUsernameRequestBuilderTest

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Tyche.Pool.Infrastructure
open Felipearpa.Type
open FsUnitTyped
open Xunit

[<Fact>]
let ``given a bet row key when built then sets only gamblerUsername`` () =
    let key =
        dict
            [ "pk", AttributeValue(S = "GAMBLER#01K1PX1TX2NM1HG851S1V0QG6B#POOL#01K1PX1TX2NM1HG851S1V0QG6A")
              "sk", AttributeValue(S = "MATCH#01K1PX1TX2NM1HG851S1V0QG6C") ]
        |> Dictionary

    let request =
        UpdateGamblerBetUsernameRequestBuilder.build key (NonEmptyString100.newOf "newname")

    request.TableName |> shouldEqual "Pool"

    request.Key["pk"].S
    |> shouldEqual "GAMBLER#01K1PX1TX2NM1HG851S1V0QG6B#POOL#01K1PX1TX2NM1HG851S1V0QG6A"

    request.Key["sk"].S |> shouldEqual "MATCH#01K1PX1TX2NM1HG851S1V0QG6C"

    request.UpdateExpression
    |> shouldEqual "SET #gamblerUsername = :gamblerUsername"

    request.ExpressionAttributeValues[":gamblerUsername"].S |> shouldEqual "newname"
