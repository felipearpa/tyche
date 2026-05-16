namespace Felipearpa.Tyche.Pool.Test.Infrastructure

open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Infrastructure
open Felipearpa.Type
open FsUnitTyped
open Xunit

module JoinPoolGamblerRequestBuilderTest =

    let private input () : ResolvedCreatePoolInput =
        { PoolId = Ulid.newOf "01K1PX1TX2NM1HG851S1V0QG6A"
          PoolName = NonEmptyString100.newOf "Polla 2026"
          PoolLayoutId = Ulid.newOf "01K1PX1TX2NM1HG851S1V0QG6B"
          PoolLayoutVersion = 7
          OwnerGamblerId = Ulid.newOf "01K1PX1TX2NM1HG851S1V0QG6C"
          OwnerGamblerUsername = NonEmptyString100.newOf "felipe@tyche.com" }

    [<Fact>]
    let ``given a resolved create pool input when built then the actual layout version is written`` () =
        let put = JoinPoolGamblerRequestBuilder.build (input ())

        put.Item["poolLayoutVersion"].N |> shouldEqual "7"

    [<Fact>]
    let ``given a resolved create pool input when built then GSI keys for fan-out are written`` () =
        let put = JoinPoolGamblerRequestBuilder.build (input ())

        put.Item["getGamblersByPoolLayoutPk"].S
        |> shouldEqual "POOLLAYOUT#01K1PX1TX2NM1HG851S1V0QG6B"

        put.Item["getGamblersByPoolLayoutSk"].S
        |> shouldEqual "POOL#01K1PX1TX2NM1HG851S1V0QG6A#GAMBLER#01K1PX1TX2NM1HG851S1V0QG6C"

    [<Fact>]
    let ``given a resolved create pool input when built then position-related attributes are absent`` () =
        let put = JoinPoolGamblerRequestBuilder.build (input ())

        put.Item.ContainsKey("beforeScore") |> shouldEqual false
        put.Item.ContainsKey("beforePosition") |> shouldEqual false
        put.Item.ContainsKey("position") |> shouldEqual false

    [<Fact>]
    let ``given a resolved create pool input when built then score is zero`` () =
        let put = JoinPoolGamblerRequestBuilder.build (input ())

        put.Item["score"].N |> shouldEqual "0"

    [<Fact>]
    let ``given a resolved create pool input when built then gamblerEmail mirrors gamblerUsername`` () =
        let put = JoinPoolGamblerRequestBuilder.build (input ())

        put.Item["gamblerEmail"].S |> shouldEqual "felipe@tyche.com"
        put.Item["gamblerUsername"].S |> shouldEqual "felipe@tyche.com"

    [<Fact>]
    let ``given a resolved create pool input when built then it has the attribute_not_exists condition`` () =
        let put = JoinPoolGamblerRequestBuilder.build (input ())

        put.ConditionExpression
        |> shouldEqual "attribute_not_exists(pk) AND attribute_not_exists(sk)"
