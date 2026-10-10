namespace Felipearpa.Tyche.Pool.Test.Settlement

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Tyche.Pool.Test.Fakes
open Felipearpa.Tyche.Pool.Test.Settlement.PoolScenario
open FsUnitTyped

/// Failure-path scenarios for result settlement and position finalization. Each documents
/// the intended outcome; they run against any ISettlementHarness implementation.
///
/// Fixture: A has 4 points (3rd), B 10 (1st), C 6 (2nd). Match 1 ends 1-0: A predicted 1-0
/// (+20), B predicted 2-2 (0), C has no bet (0). Match 2 ends 0-0: A predicted 0-0 (+20),
/// B 1-0 (+4), C 3-1 (0).
module SettlementScenarios =

    let private keyOf (item: TransactWriteItem) : IDictionary<string, AttributeValue> =
        if not (isNull item.Update) then item.Update.Key
        elif not (isNull item.Put) then item.Put.Item
        elif not (isNull item.Delete) then item.Delete.Key
        else item.ConditionCheck.Key

    let private touches (pk: string) (sk: string) (key: IDictionary<string, AttributeValue>) =
        key["pk"].S = pk && key["sk"].S = sk

    let private detailKey gamblerId matchId =
        $"GAMBLER#{gamblerId}#POOL#{poolId}", $"MATCH#{matchId}"

    /// True when the request writes the given detail and its update expression satisfies the predicate.
    let private writesDetail gamblerId matchId (expressionMatches: string -> bool) (request: obj) =
        let pk, sk = detailKey gamblerId matchId

        match request with
        | :? TransactWriteItemsRequest as transaction ->
            transaction.TransactItems
            |> Seq.exists (fun item ->
                not (isNull item.Update)
                && touches pk sk (keyOf item)
                && expressionMatches item.Update.UpdateExpression)
        | :? UpdateItemRequest as update -> touches pk sk update.Key && expressionMatches update.UpdateExpression
        | _ -> false

    /// Injects the fault into the first matching request and returns a flag recording that it fired.
    let private once (fake: FakeDynamoDb) (predicate: obj -> bool) (fault: FakeFault) =
        let fired = ref false

        fake.Intercept(fun request ->
            if not fired.Value && predicate request then
                fired.Value <- true
                fault
            else
                Proceed)

        fired

    /// Runs a stream delivery, retrying it once after a failure as the event source would.
    let private handleWithRetry (harness: ISettlementHarness) records =
        async {
            let! first = harness.HandleLayoutAsync records |> Async.Catch

            match first with
            | Choice1Of2() -> ()
            | Choice2Of2 _ -> do! harness.HandleLayoutAsync records
        }

    let private settleMatch1 (harness: ISettlementHarness) =
        async {
            do! handleWithRetry harness [ ResultChanged(match1, 1, 0) ]
            do! harness.DeliverAsync()
        }

    let private assertMatch1Settled (fake: FakeDynamoDb) =
        [ total fake gamblerA; total fake gamblerB; total fake gamblerC ]
        |> shouldEqual [ Some 24; Some 10; Some 6 ]

        [ award fake gamblerA match1; award fake gamblerB match1; award fake gamblerC match1 ]
        |> shouldEqual [ Some 20; Some 0; Some 0 ]

    let private assertMatch1Positions (fake: FakeDynamoDb) =
        [ position fake gamblerA; position fake gamblerB; position fake gamblerC ]
        |> shouldEqual [ Some 1; Some 2; Some 3 ]

        [ beforePosition fake gamblerA; beforePosition fake gamblerB; beforePosition fake gamblerC ]
        |> shouldEqual [ Some 3; Some 1; Some 2 ]

        [ detailPosition fake gamblerA match1
          detailPosition fake gamblerB match1
          detailPosition fake gamblerC match1 ]
        |> shouldEqual [ Some 1; Some 2; Some 3 ]

        [ detailBeforePosition fake gamblerA match1
          detailBeforePosition fake gamblerB match1
          detailBeforePosition fake gamblerC match1 ]
        |> shouldEqual [ Some 3; Some 1; Some 2 ]

    /// Intended: an enqueue failure after scoring may not lose position finalization; the
    /// retried source event rediscovers the pool even though its bets are already computed.
    let lostPostCommitNotification (harness: ISettlementHarness) =
        async {
            do! harness.SeedAsync(defaultMembers, defaultMatches)
            harness.Queue.FailSendWhen(fun _ -> Some(exn "SQS unavailable"))
            let! first = harness.HandleLayoutAsync [ ResultChanged(match1, 1, 0) ] |> Async.Catch
            first |> function Choice2Of2 _ -> () | Choice1Of2 _ -> failwith "expected the enqueue failure to surface"
            harness.Queue.ClearSendFailures()

            do! harness.HandleLayoutAsync [ ResultChanged(match1, 1, 0) ]
            do! harness.DeliverAsync()

            assertMatch1Settled harness.Fake
            assertMatch1Positions harness.Fake
        }

    /// Intended: a retryable transaction cancellation leaves the bet pending for retry and is
    /// never reported as settled.
    let cancellationIsNotSettlement (harness: ISettlementHarness) =
        async {
            do! harness.SeedAsync(defaultMembers, defaultMatches)

            let fired =
                once
                    harness.Fake
                    (writesDetail gamblerB match1 (fun expression -> expression.Contains "computedRequestId"))
                    (FailBefore(
                        TransactionCanceledException(
                            "Transaction cancelled [TransactionConflict, None]",
                            CancellationReasons =
                                List([ CancellationReason(Code = "TransactionConflict"); CancellationReason(Code = "None") ])
                        )
                    ))

            do! settleMatch1 harness

            fired.Value |> shouldEqual true
            isComputed harness.Fake gamblerB match1 |> shouldEqual true
            assertMatch1Settled harness.Fake
            assertMatch1Positions harness.Fake
        }

    /// Intended: an interrupted position pass resumes without losing the original previous
    /// positions on either the membership or the match detail.
    let partialPositionWriteKeepsPreviousPositions (harness: ISettlementHarness) =
        async {
            do! harness.SeedAsync(defaultMembers, defaultMatches)

            let fired =
                once
                    harness.Fake
                    (writesDetail gamblerC match1 (fun expression -> expression.Contains "#position"))
                    (FailBefore(ProvisionedThroughputExceededException "Throughput exceeded"))

            do! settleMatch1 harness

            fired.Value |> shouldEqual true
            assertMatch1Settled harness.Fake
            assertMatch1Positions harness.Fake
        }

    /// Intended: replaying an older completed operation after a newer one changes nothing.
    let olderOperationReplayIsNoOp (harness: ISettlementHarness) =
        async {
            do! harness.SeedAsync(defaultMembers, defaultMatches)
            do! settleMatch1 harness
            do! handleWithRetry harness [ ResultChanged(match2, 0, 0) ]
            do! harness.DeliverAsync()

            let beforeReplay = [ gamblerA; gamblerB; gamblerC ] |> List.map (beforePosition harness.Fake)

            do! harness.RedeliverAsync match1
            do! harness.DeliverAsync()

            [ total harness.Fake gamblerA; total harness.Fake gamblerB; total harness.Fake gamblerC ]
            |> shouldEqual [ Some 44; Some 14; Some 6 ]

            [ gamblerA; gamblerB; gamblerC ]
            |> List.map (beforePosition harness.Fake)
            |> shouldEqual beforeReplay

            [ detailBeforePosition harness.Fake gamblerA match1
              detailBeforePosition harness.Fake gamblerB match1
              detailBeforePosition harness.Fake gamblerC match1 ]
            |> shouldEqual [ Some 3; Some 1; Some 2 ]
        }

    /// Intended: positions come from authoritative totals even while the leaderboard index lags.
    let positionsIgnoreLaggingLeaderboardIndex (harness: ISettlementHarness) =
        async {
            do! harness.SeedAsync(defaultMembers, defaultMatches)
            harness.Fake.FreezeIndexes()

            do! settleMatch1 harness

            harness.Fake.ReleaseIndexes()
            assertMatch1Settled harness.Fake
            assertMatch1Positions harness.Fake
        }

    let private match3Seed result =
        { MatchId = match3
          MatchDateTime = pastKickoff.AddHours(1.0)
          Result = result }

    let private assertMatch3Computed (fake: FakeDynamoDb) =
        [ gamblerA; gamblerB; gamblerC ]
        |> List.map (fun gamblerId -> isComputed fake gamblerId match3, award fake gamblerId match3)
        |> shouldEqual [ true, Some 0; true, Some 0; true, Some 0 ]

        [ total fake gamblerA; total fake gamblerB; total fake gamblerC ]
        |> shouldEqual [ Some 4; Some 10; Some 6 ]

    /// Intended: a match inserted with its final result already set is settled for every
    /// eligible detail even though no result-change event follows.
    let matchInsertedWithResultIsSettled (harness: ISettlementHarness) =
        async {
            do! harness.SeedAsync(defaultMembers, defaultMatches)
            do! handleWithRetry harness [ MatchInserted(match3Seed (Some(2, 1))) ]
            do! harness.DeliverAsync()

            assertMatch3Computed harness.Fake
        }

    /// Intended: an insertion and its result in one delivery converge regardless of the
    /// handler's processing order.
    let insertionAndResultInOneDeliveryConverge (harness: ISettlementHarness) =
        async {
            do! harness.SeedAsync(defaultMembers, defaultMatches)

            do! handleWithRetry harness [ MatchInserted(match3Seed None); ResultChanged(match3, 2, 1) ]
            do! harness.DeliverAsync()

            assertMatch3Computed harness.Fake
        }

    /// Intended: a settlement interrupted by a throttled write resumes and applies every award once.
    let interruptedSettlementResumes (harness: ISettlementHarness) =
        async {
            do! harness.SeedAsync(defaultMembers, defaultMatches)

            let fired =
                once
                    harness.Fake
                    (writesDetail gamblerC match1 (fun expression -> expression.Contains "computedRequestId"))
                    (FailBefore(ProvisionedThroughputExceededException "Throughput exceeded"))

            do! settleMatch1 harness

            fired.Value |> shouldEqual true
            assertMatch1Settled harness.Fake
            assertMatch1Positions harness.Fake
        }
