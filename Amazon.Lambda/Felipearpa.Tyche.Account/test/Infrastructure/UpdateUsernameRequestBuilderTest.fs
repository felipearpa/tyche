module UpdateUsernameRequestBuilderTest

open Felipearpa.Tyche.Account.Infrastructure
open Felipearpa.Type
open FsUnitTyped
open Xunit

[<Fact>]
let ``given account id and username when built then targets the account partition`` () =
    let request =
        UpdateUsernameRequestBuilder.build (Ulid.newOf "01K1PX1TX2NM1HG851S1V0QG6A") (NonEmptyString100.newOf "newname")

    request.TableName |> shouldEqual "Account"
    request.Key["pk"].S |> shouldEqual "ACCOUNT#01K1PX1TX2NM1HG851S1V0QG6A"

[<Fact>]
let ``given a username when built then sets username via expression`` () =
    let request =
        UpdateUsernameRequestBuilder.build (Ulid.newOf "01K1PX1TX2NM1HG851S1V0QG6A") (NonEmptyString100.newOf "newname")

    request.UpdateExpression |> shouldEqual "SET #username = :username"
    request.ExpressionAttributeValues[":username"].S |> shouldEqual "newname"
