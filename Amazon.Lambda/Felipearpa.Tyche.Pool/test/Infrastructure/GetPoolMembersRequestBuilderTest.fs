namespace Felipearpa.Tyche.Pool.Test.Infrastructure

open Felipearpa.Tyche.Pool.Infrastructure
open Felipearpa.Type
open FsUnitTyped
open Xunit

module GetPoolMembersRequestBuilderTest =

    let private poolId = Ulid.newOf "01K1PX1TX2NM1HG851S1V0QG6A"

    [<Fact>]
    let ``given a pool id when built then it targets the gamblers-by-username index`` () =
        let request = GetPoolMembersRequestBuilder.build poolId None

        request.IndexName |> shouldEqual "GetPoolGamblersByUsername-index"

    [<Fact>]
    let ``given a pool id when built then it scans ascending by username`` () =
        let request = GetPoolMembersRequestBuilder.build poolId None

        request.ScanIndexForward.Value |> shouldEqual true

    [<Fact>]
    let ``given a pool id when built then it partitions on the pool key`` () =
        let request = GetPoolMembersRequestBuilder.build poolId None

        request.ExpressionAttributeValues[":pk"].S
        |> shouldEqual "POOL#01K1PX1TX2NM1HG851S1V0QG6A"
