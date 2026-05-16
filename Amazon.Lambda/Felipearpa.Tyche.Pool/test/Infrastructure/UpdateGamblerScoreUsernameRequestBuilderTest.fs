module UpdateGamblerScoreUsernameRequestBuilderTest

open Felipearpa.Tyche.Pool.Infrastructure
open Felipearpa.Type
open FsUnitTyped
open Xunit

[<Fact>]
let ``given a score row key when built then targets the row and rewrites filter`` () =
    let request =
        UpdateGamblerScoreUsernameRequestBuilder.build
            "POOL#01K1PX1TX2NM1HG851S1V0QG6A"
            "GAMBLER#01K1PX1TX2NM1HG851S1V0QG6B"
            "Polla 2026"
            (NonEmptyString100.newOf "newname")

    request.TableName |> shouldEqual "Pool"
    request.Key["pk"].S |> shouldEqual "POOL#01K1PX1TX2NM1HG851S1V0QG6A"
    request.Key["sk"].S |> shouldEqual "GAMBLER#01K1PX1TX2NM1HG851S1V0QG6B"

    request.UpdateExpression
    |> shouldEqual "SET #gamblerUsername = :gamblerUsername, #filter = :filter"

    request.ExpressionAttributeValues[":gamblerUsername"].S |> shouldEqual "newname"

    request.ExpressionAttributeValues[":filter"].S
    |> shouldEqual "Polla 2026 newname"
