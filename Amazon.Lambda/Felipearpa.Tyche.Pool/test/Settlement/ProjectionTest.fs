namespace Felipearpa.Tyche.Pool.Test.Settlement

open System
open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Test.Fakes
open Felipearpa.Tyche.Pool.Test.Settlement.PoolScenario
open Felipearpa.Type
open FsUnitTyped
open Xunit

/// Propagation of absolute versioned totals, membership removal, and rejoin fencing.
module ProjectionTest =

    let private pool = Ulid.newOf poolId

    let private projectionAttributes =
        set
            [ "totalPoolPoints"
              "totalPoolPointsState"
              "poolScoreVersion"
              "getPoolMatchBetsByTotalSk"
              "position"
              "beforePosition"
              "positionOperationRevision"
              "positionCalculationRevision" ]

    let private withoutProjection (item: Dictionary<string, AttributeValue> option) =
        item
        |> Option.map (fun found ->
            found
            |> Seq.filter (fun pair -> not (projectionAttributes.Contains pair.Key))
            |> Seq.map (fun pair -> pair.Key, (if isNull pair.Value.S then pair.Value.N else pair.Value.S))
            |> List.ofSeq
            |> List.sort)

    let private migrated () =
        async {
            let harness = VersionedSettlementHarness()
            do! harness.SeedAndMigrateAsync(defaultMembers, defaultMatches)
            return harness
        }

    [<Fact>]
    let ``a new total reaches every match detail of the member, including historical ones, and nothing else changes`` () =
        async {
            let! harness = migrated ()
            let historicalBefore = detail harness.Fake gamblerA match0 |> withoutProjection
            let pendingBefore = detail harness.Fake gamblerA match2 |> withoutProjection

            do! harness.SettleAsync(match1, 1, 0)

            for matchId in [ match0; match1; match2 ] do
                detail harness.Fake gamblerA matchId |> number "totalPoolPoints" |> shouldEqual (Some 24)

                detail harness.Fake gamblerA matchId |> number "poolScoreVersion"
                |> shouldEqual (membership harness.Fake gamblerA |> number "scoreVersion")

                detail harness.Fake gamblerA matchId |> text "getPoolMatchBetsByTotalSk"
                |> shouldEqual (Some(RankingKey.sortKey (KnownTotal 24) (Ulid.newOf gamblerA)))

            detail harness.Fake gamblerA match0 |> withoutProjection |> shouldEqual historicalBefore
            detail harness.Fake gamblerA match2 |> withoutProjection |> shouldEqual pendingBefore
            detail harness.Fake gamblerA match0 |> number "score" |> shouldEqual (Some 4)
        }

    [<Fact>]
    let ``an unchanged total produces no propagation work`` () =
        async {
            let! harness = migrated ()
            let before = detail harness.Fake gamblerC match0 |> number "poolScoreVersion"

            do! harness.SettleAsync(match1, 1, 0)

            detail harness.Fake gamblerC match0 |> number "poolScoreVersion" |> shouldEqual before
            detail harness.Fake gamblerC match0 |> number "totalPoolPoints" |> shouldEqual (Some 6)
        }

    [<Fact>]
    let ``duplicate, stale, and reversed propagation jobs converge on the latest total`` () =
        async {
            let! harness = migrated ()
            do! harness.SettleAsync(match1, 1, 0)
            let! _ = harness.CorrectResultAsync(match1, 2, 0)
            do! harness.RunUntilQuiescentAsync()

            // Replays of older jobs read the latest total and fail their version guards.
            do!
                harness.RunJobsAsync
                    [ PropagateTotal(pool, Ulid.newOf gamblerA, None)
                      PropagateTotal(pool, Ulid.newOf gamblerA, None)
                      PropagateTotal(pool, Ulid.newOf gamblerA, Some match0) ]

            // A newer decrease (24 → 18) replaced the older higher total.
            for matchId in [ match0; match1; match2 ] do
                detail harness.Fake gamblerA matchId |> number "totalPoolPoints" |> shouldEqual (Some 18)
        }

    [<Fact>]
    let ``an older total delivered after a newer one never replaces it`` () =
        async {
            let! harness = migrated ()
            do! harness.SettleAsync(match1, 1, 0)

            let binding =
                { Generation = membership harness.Fake gamblerA |> text "membershipGeneration" |> Option.get
                  ScoreVersion = 1L
                  Total = KnownTotal 4 }

            let! outcome = harness.Projection.ApplyTotalAsync(pool, Ulid.newOf gamblerA, Ulid.newOf match0, binding)

            outcome |> shouldEqual ConditionNotMet
            detail harness.Fake gamblerA match0 |> number "totalPoolPoints" |> shouldEqual (Some 24)
        }

    [<Fact>]
    let ``a failed propagation write is retried, not swallowed`` () =
        async {
            let! harness = migrated ()
            let failures = ref 0

            harness.Fake.Intercept(fun request ->
                match request with
                | :? UpdateItemRequest as update when
                    failures.Value < 1 && update.UpdateExpression.Contains "#poolScoreVersion = :poolScoreVersion"
                    ->
                    failures.Value <- failures.Value + 1
                    FailBefore(ProvisionedThroughputExceededException "Throughput exceeded")
                | _ -> Proceed)

            do! harness.SettleAsync(match1, 1, 0)

            failures.Value |> shouldEqual 1

            for matchId in [ match0; match1; match2 ] do
                detail harness.Fake gamblerA matchId |> number "totalPoolPoints" |> shouldEqual (Some 24)
        }

    [<Fact>]
    let ``long bet sheets continue through durable continuations`` () =
        async {
            let harness = VersionedSettlementHarness()

            let manyMatches =
                [ for index in 1..520 ->
                      { MatchId = $"01K1PX1TX2NM1HG851S1V{index:D5}"
                        MatchDateTime = pastKickoff.AddDays(float index)
                        Result = None } ]

            do! harness.SeedAndMigrateAsync([ defaultMembers.Head ], manyMatches)
            let current = membership harness.Fake gamblerA |> Option.get
            current["score"] <- AttributeValue(N = "99")
            current["scoreVersion"] <- AttributeValue(N = string (int current["scoreVersion"].N + 1))
            harness.Fake.Client.PutItemAsync(PutItemRequest(TableName = "Pool", Item = current)).Wait()

            do! harness.RunUntilQuiescentAsync()

            harness.Fake.Items("Pool")
            |> List.filter (fun item -> item["pk"].S.StartsWith $"GAMBLER#{gamblerA}")
            |> List.map (fun item -> item["totalPoolPoints"].N)
            |> List.distinct
            |> shouldEqual [ "99" ]
        }

    [<Fact>]
    let ``removal deletes the generation's uncomputed details and retires its computed ones`` () =
        async {
            let! harness = migrated ()
            let generation = membership harness.Fake gamblerC |> text "membershipGeneration"

            do! harness.RemoveAsync gamblerC
            do! harness.RunUntilQuiescentAsync()

            detail harness.Fake gamblerC match1 |> shouldEqual None
            detail harness.Fake gamblerC match2 |> shouldEqual None
            let retained = detail harness.Fake gamblerC match0
            retained |> number "score" |> shouldEqual (Some 6)
            retained |> has "getPoolMatchBetsByTotalPk" |> shouldEqual false
            retained |> has "getPoolMatchBetsByTotalSk" |> shouldEqual false
            retained |> text "poolMembershipGeneration" |> shouldEqual generation
            retained |> Option.map (fun item -> item["rankingActive"].BOOL.Value) |> shouldEqual (Some false)
        }

    [<Fact>]
    let ``an old removal or score job cannot touch a rejoined membership`` () =
        async {
            let! harness = migrated ()
            let oldGeneration = membership harness.Fake gamblerC |> text "membershipGeneration"
            do! harness.RemoveAsync gamblerC
            do! harness.RunUntilQuiescentAsync()
            let! _ = harness.JoinAsync gamblerC
            do! harness.RunUntilQuiescentAsync()

            let newGeneration = membership harness.Fake gamblerC |> text "membershipGeneration"
            newGeneration |> shouldNotEqual oldGeneration
            detail harness.Fake gamblerC match1 |> text "poolMembershipGeneration" |> shouldEqual newGeneration

            // The delayed removal of the old membership runs again.
            let cleanUp = Felipearpa.Tyche.Pool.Application.CleanUpRemovedMembership(harness.Projection)
            do! cleanUp.ExecuteAsync(pool, Ulid.newOf gamblerC, oldGeneration)

            detail harness.Fake gamblerC match1 |> has "getPoolMatchBetsByTotalSk" |> shouldEqual true
            detail harness.Fake gamblerC match2 |> Option.isSome |> shouldEqual true

            // The retained detail of the old membership is not reactivated or rebound.
            detail harness.Fake gamblerC match0 |> text "poolMembershipGeneration" |> shouldEqual oldGeneration
            detail harness.Fake gamblerC match0 |> has "getPoolMatchBetsByTotalSk" |> shouldEqual false

            // A delayed score of the old membership cannot charge the new one.
            do! harness.SettleAsync(match1, 1, 0)
            total harness.Fake gamblerC |> shouldEqual (Some 0)
        }

    [<Fact>]
    let ``a removal recorded before generations existed only deletes unbound uncomputed details`` () =
        async {
            let! harness = migrated ()
            let cleanUp = Felipearpa.Tyche.Pool.Application.CleanUpRemovedMembership(harness.Projection)

            do! cleanUp.ExecuteAsync(pool, Ulid.newOf gamblerB, None)

            detail harness.Fake gamblerB match1 |> Option.isSome |> shouldEqual true
            detail harness.Fake gamblerB match2 |> Option.isSome |> shouldEqual true
        }

    [<Fact>]
    let ``a removed membership's details are not recreated by a later materialization`` () =
        async {
            let! harness = migrated ()
            do! harness.RemoveAsync gamblerB
            do! harness.RunUntilQuiescentAsync()

            do!
                harness.HandleLayoutRecordsAsync
                    [ MatchInserted
                          { MatchId = match3
                            MatchDateTime = pastKickoff.AddHours 1.0
                            Result = None } ]

            detail harness.Fake gamblerB match3 |> shouldEqual None
        }
