namespace Felipearpa.Tyche.Pool.Test.Application

open System
open Amazon.DynamoDBv2.Model
open Felipearpa.Tyche.Pool.Application
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Test.Settlement
open Felipearpa.Tyche.Pool.Test.Settlement.PoolScenario
open Felipearpa.Type
open FsUnitTyped
open Xunit

/// Added-match materialization through the guarded initializer, against the fake table.
module FanOutMatchToGamblersTest =

    let private addedMatch result =
        { MatchId = match3
          MatchDateTime = pastKickoff.AddDays(1.0)
          Result = result }

    let private migrated () =
        async {
            let harness = VersionedSettlementHarness()
            do! harness.SeedAndMigrateAsync(defaultMembers, defaultMatches)
            return harness
        }

    let private receiptState (harness: VersionedSettlementHarness) operationId =
        harness.Fake.TryGet(
            "Pool",
            dict [ "pk", AttributeValue(S = $"POOL#{poolId}"); "sk", AttributeValue(S = $"SETTLEMENT#{operationId}") ]
        )
        |> text "operationState"

    [<Fact>]
    let ``given an added match when fanned out then every member gets a bound uncomputed detail with the current total`` () =
        async {
            let! harness = migrated ()

            do! harness.HandleLayoutRecordsAsync [ MatchInserted(addedMatch None) ]

            for gamblerId, total in [ gamblerA, 4; gamblerB, 10; gamblerC, 6 ] do
                let created = detail harness.Fake gamblerId match3
                created |> has "computedRequestId" |> shouldEqual false
                created |> number "totalPoolPoints" |> shouldEqual (Some total)
                created |> text "totalPoolPointsState" |> shouldEqual (Some "KNOWN")

                created |> text "poolMembershipGeneration"
                |> shouldEqual (membership harness.Fake gamblerId |> text "membershipGeneration")

                created |> text "getPoolMatchBetsByTotalPk" |> shouldEqual (Some $"POOL#{poolId}#MATCH#{match3}")

            harness.Commands.Pending |> shouldEqual []
        }

    [<Fact>]
    let ``given an added match that already has a result when fanned out then a reconciliation named after the insert is requested`` () =
        async {
            let! harness = migrated ()

            do! harness.HandleLayoutRecordsAsync [ MatchInserted(addedMatch (Some(1, 1))) ]

            harness.Commands.Pending
            |> List.map _.DeduplicationId
            |> shouldEqual [ $"{poolId}#MATCH#{match3}#INSERT#{match3}" ]
        }

    [<Fact>]
    let ``given a result already settled for the pool when its match is fanned out late then the new details are still settled`` () =
        async {
            let! harness = migrated ()
            let pool = Ulid.newOf poolId

            // The result was processed before the match rows existed, so its own operation
            // completed with every detail pending materialization.
            do!
                harness.CommandPublisher.PublishAsync
                    [ PoolCommand.settleResult pool (Ulid.newOf match3) { HomeTeamScore = 1; AwayTeamScore = 1; Version = 1 } ]

            do! harness.RunUntilQuiescentAsync()
            receiptState harness $"RESULT#{match3}#1" |> shouldEqual (Some "COMPLETED")
            detail harness.Fake gamblerA match3 |> shouldEqual None

            do! harness.HandleLayoutRecordsAsync [ MatchInserted(addedMatch (Some(1, 1))) ]
            do! harness.RunUntilQuiescentAsync()

            receiptState harness $"MATCH#{match3}#INSERT#{match3}" |> shouldEqual (Some "COMPLETED")

            for gamblerId in [ gamblerA; gamblerB; gamblerC ] do
                isComputed harness.Fake gamblerId match3 |> shouldEqual true
                award harness.Fake gamblerId match3 |> shouldEqual (Some 0)
        }

    [<Fact>]
    let ``given a member removed before fan-out when fanned out then no detail is recreated for them`` () =
        async {
            let! harness = migrated ()
            do! harness.RemoveAsync gamblerC
            do! harness.RunUntilQuiescentAsync()

            do! harness.HandleLayoutRecordsAsync [ MatchInserted(addedMatch None) ]

            detail harness.Fake gamblerC match3 |> shouldEqual None
            detail harness.Fake gamblerA match3 |> Option.isSome |> shouldEqual true
        }
