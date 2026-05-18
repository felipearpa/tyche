namespace Felipearpa.Tyche.Pool.Test.Infrastructure

open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Infrastructure
open Felipearpa.Type
open FsUnitTyped
open Xunit

module CreatePoolRequestBuilderTest =

    let private input () : ResolvedCreatePoolInput =
        { PoolId = Ulid.newOf "01K1PX1TX2NM1HG851S1V0QG6A"
          PoolName = NonEmptyString100.newOf "Polla 2026"
          PoolLayoutId = Ulid.newOf "01K1PX1TX2NM1HG851S1V0QG6B"
          PoolLayoutVersion = 7
          OwnerGamblerId = Ulid.newOf "01K1PX1TX2NM1HG851S1V0QG6C"
          OwnerGamblerUsername = NonEmptyString100.newOf "felipe@tyche.com"
          OwnerGamblerEmail = NonEmptyString100.newOf "felipe@tyche.com" }

    [<Fact>]
    let ``given a resolved create pool input when built then the pool root has gamblerCount initialized to 1`` () =
        let put = CreatePoolRequestBuilder.build (input ())

        put.Item["gamblerCount"].N |> shouldEqual "1"

    [<Fact>]
    let ``given a resolved create pool input when built then the pool root stores the owner as the creator gambler``
        ()
        =
        let resolved = input ()

        let put = CreatePoolRequestBuilder.build resolved

        put.Item["creatorGamblerId"].S
        |> shouldEqual (resolved.OwnerGamblerId |> Ulid.value)
