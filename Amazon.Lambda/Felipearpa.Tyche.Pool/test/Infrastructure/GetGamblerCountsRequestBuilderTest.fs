namespace Felipearpa.Tyche.Pool.Test.Infrastructure

open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.Pool.Infrastructure
open Felipearpa.Type
open FsUnitTyped
open Xunit

module GetGamblerCountsRequestBuilderTest =

    let private poolIds =
        [ Ulid.newOf "01K1PX1TX2NM1HG851S1V0QG6A"
          Ulid.newOf "01K1PX1TX2NM1HG851S1V0QG6B" ]

    [<Fact>]
    let ``given pool ids when built then it targets the pool table`` () =
        let request = GetGamblerCountsRequestBuilder.build poolIds

        request.RequestItems.ContainsKey PoolTable.name |> shouldEqual true

    [<Fact>]
    let ``given pool ids when built then it requests one pool root key per pool`` () =
        let request = GetGamblerCountsRequestBuilder.build poolIds

        let keys = request.RequestItems[PoolTable.name].Keys

        keys.Count |> shouldEqual 2

        keys
        |> Seq.map (fun key -> key[Key.pk].S, key[Key.sk].S)
        |> Seq.toList
        |> shouldEqual
            [ "POOL#01K1PX1TX2NM1HG851S1V0QG6A", "POOL#01K1PX1TX2NM1HG851S1V0QG6A"
              "POOL#01K1PX1TX2NM1HG851S1V0QG6B", "POOL#01K1PX1TX2NM1HG851S1V0QG6B" ]

    [<Fact>]
    let ``given pool ids when built then it projects only the pool id and the gambler count`` () =
        let request = GetGamblerCountsRequestBuilder.build poolIds

        request.RequestItems[PoolTable.name].ProjectionExpression
        |> shouldEqual "#poolId, #gamblerCount"
