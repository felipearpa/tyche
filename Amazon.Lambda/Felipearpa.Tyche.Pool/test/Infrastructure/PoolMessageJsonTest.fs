namespace Felipearpa.Tyche.Pool.Test.Infrastructure

open System.IO
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Infrastructure
open Felipearpa.Type
open FsUnitTyped
open Xunit

module PoolMessageJsonTest =

    let private poolId = Ulid.newOf "01K1PX1TX2NM1HG851S1V0P001"
    let private matchId = Ulid.newOf "01K1PX1TX2NM1HG851S1V0M001"
    let private gamblerId = Ulid.newOf "01K1PX1TX2NM1HG851S1V0GD01"

    [<Fact>]
    let ``versioned commands round-trip with stable operation identities`` () =
        let result =
            { HomeTeamScore = 2
              AwayTeamScore = 1
              Version = 3 }

        for command in
            [ PoolCommand.settleResult poolId matchId result
              PoolCommand.reconcileMatch poolId matchId "EVENT-1"
              PoolCommand.reconcileStandings poolId "EVENT-2"
              PoolCommand.restoreRejoinHistory poolId gamblerId "G2" ] do
            match PoolMessageJson.encodeCommand command |> PoolMessageJson.decodePoolMessage with
            | PoolMessageJson.Command decoded -> decoded |> shouldEqual command
            | other -> failwith $"unexpected {other}"

        (PoolCommand.settleResult poolId matchId result).OperationId
        |> shouldEqual $"RESULT#{matchId.Value}#3"

        (PoolCommand.restoreRejoinHistory poolId gamblerId "G2").OperationId
        |> shouldEqual $"RESTORE#{gamblerId.Value}#G2"

    [<Fact>]
    let ``an operator's restore replay decodes with its own operation ID and the same payload`` () =
        let body =
            $"{{\"schema\":1,\"operationId\":\"RESTORE#{gamblerId.Value}#G2#OPERATOR-1\",\"poolId\":\"{poolId.Value}\",\"kind\":\"RESTORE_REJOIN_HISTORY\",\"gamblerId\":\"{gamblerId.Value}\",\"membershipGeneration\":\"G2\"}}"

        match PoolMessageJson.decodePoolMessage body with
        | PoolMessageJson.Command decoded ->
            decoded.OperationId |> shouldEqual $"RESTORE#{gamblerId.Value}#G2#OPERATOR-1"
            decoded.Kind |> shouldEqual (RestoreRejoinHistory(gamblerId, "G2"))
            PoolCommand.fingerprint decoded |> shouldEqual (PoolCommand.fingerprint (PoolCommand.restoreRejoinHistory poolId gamblerId "G2"))
        | other -> failwith $"unexpected {other}"

    [<Fact>]
    let ``messages from the previous position worker decode as legacy positions`` () =
        PoolMessageJson.decodePoolMessage $"{poolId.Value}|{matchId.Value}"
        |> shouldEqual (PoolMessageJson.LegacyPositions(poolId, matchId))

    [<Fact>]
    let ``an unknown schema is rejected instead of guessed`` () =
        Assert.Throws<InvalidDataException>(fun () ->
            PoolMessageJson.decodePoolMessage """{"schema":2,"kind":"RECONCILE_STANDINGS"}""" |> ignore)
        |> ignore

    [<Fact>]
    let ``maintenance jobs round-trip including continuations`` () =
        let gamblerId = Ulid.newOf "01K1PX1TX2NM1HG851S1V0GA01"

        for job in
            [ PropagateTotal(poolId, gamblerId, Some "01K1PX1TX2NM1HG851S1V0M001")
              PropagateTotal(poolId, gamblerId, None)
              InitializeIdentities(poolId, Some "x")
              BackfillProjection(poolId, None)
              VerifyReadiness poolId
              ReconcilePool(poolId, None)
              RetireMembership(poolId, gamblerId, Some "G1")
              RetireMembership(poolId, gamblerId, None) ] do
            PoolMessageJson.encodeJob job |> PoolMessageJson.decodeJob |> shouldEqual job

    [<Fact>]
    let ``long operation IDs get a deduplication ID within the SQS limit`` () =
        let command =
            { PoolCommand.reconcileStandings poolId (String.replicate 200 "x") with OperationId = String.replicate 200 "x" }

        (PoolMessageJson.deduplicationId command).Length |> shouldEqual 64
