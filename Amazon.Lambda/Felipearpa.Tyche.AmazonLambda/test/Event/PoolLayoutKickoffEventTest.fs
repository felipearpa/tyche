namespace Felipearpa.Tyche.AmazonLambda.Event.Tests

#nowarn "3536"

open System
open System.Collections.Generic
open Amazon.DynamoDBv2
open Amazon.DynamoDBv2.Model
open Amazon.Lambda.DynamoDBEvents
open Amazon.Lambda.TestUtilities
open Amazon.SQS
open Felipearpa.Tyche.AmazonLambda.Event
open Felipearpa.Tyche.Pool.Test.Fakes
open FsUnitTyped
open Microsoft.Extensions.DependencyInjection
open Moq
open Xunit

/// The PoolLayout stream handler copies a rescheduled match's kickoff onto every gambler's
/// detail in every pool, and a redelivered record writes nothing.
module PoolLayoutKickoffEventTest =

    let private layoutId = "01K1PX1TX2NM1HG851S1V0Y001"
    let private matchId = "01K1PX1TX2NM1HG851S1V0M001"
    let private pendingPool = "01K1PX1TX2NM1HG851S1V0P001"
    let private settledPool = "01K1PX1TX2NM1HG851S1V0P002"
    let private gamblerId = "01K1PX1TX2NM1HG851S1V0GA01"
    let private previous = "2026-06-13T16:00:00.0000000Z"
    let private postponed = "2026-06-14T16:00:00.0000000Z"
    let private s (value: string) = AttributeValue(S = value)

    let private detail (poolId: string) (computed: bool) =
        let item =
            Dictionary(
                dict
                    [ "pk", s $"GAMBLER#{gamblerId}#POOL#{poolId}"
                      "sk", s $"MATCH#{matchId}"
                      "poolId", s poolId
                      "gamblerId", s gamblerId
                      "matchId", s matchId
                      "matchDateTime", s previous
                      "getPoolGamblerScoresByMatchPk", s $"MATCH#{matchId}"
                      "getPoolGamblerScoresByMatchSk", s $"POOL#{poolId}#GAMBLER#{gamblerId}" ]
            )

        if computed then
            item["computedRequestId"] <- s "01K1PX1TX2NM1HG851S1V0R000"
            item["computedMatchDateTime"] <- s previous

        item

    let private record () =
        let image (kickoff: string) =
            Dictionary(
                dict
                    [ "pk", DynamoDBEvent.AttributeValue(S = $"POOLLAYOUT#{layoutId}")
                      "sk", DynamoDBEvent.AttributeValue(S = $"MATCH#{matchId}")
                      "matchId", DynamoDBEvent.AttributeValue(S = matchId)
                      "poolLayoutId", DynamoDBEvent.AttributeValue(S = layoutId)
                      "matchDateTime", DynamoDBEvent.AttributeValue(S = kickoff) ]
            )

        DynamoDBEvent.DynamodbStreamRecord(
            EventID = "EVENT1",
            EventName = "MODIFY",
            Dynamodb = DynamoDBEvent.StreamRecord(SequenceNumber = "1", OldImage = image previous, NewImage = image postponed)
        )

    let private deliver (fake: FakeDynamoDb) =
        async {
            let handler =
                PoolLayoutEvent(fun services ->
                    services
                        .AddSingleton<IAmazonDynamoDB>(fake.Client)
                        .AddSingleton<IAmazonSQS>(Mock<IAmazonSQS>().Object)
                    |> ignore)

            let event = DynamoDBEvent(Records = List [ record () ])
            return! handler.OnPoolLayoutChangeAsync(event, TestLambdaContext()) |> Async.AwaitTask
        }

    let private kickoffs (fake: FakeDynamoDb) (poolId: string) =
        let item =
            fake.TryGet("Pool", dict [ "pk", s $"GAMBLER#{gamblerId}#POOL#{poolId}"; "sk", s $"MATCH#{matchId}" ])
            |> Option.get

        let text name =
            match item.TryGetValue name with
            | true, value -> Some value.S
            | _ -> None

        text "matchDateTime", text "computedMatchDateTime"

    [<Fact>]
    let ``a rescheduled match moves every detail's kickoff copies and a redelivery writes nothing`` () =
        async {
            let fake = FakeTables.create ()
            fake.Put("Pool", detail pendingPool false)
            fake.Put("Pool", detail settledPool true)
            fake.TakeChanges() |> ignore

            let! response = deliver fake

            response.BatchItemFailures |> shouldBeEmpty
            kickoffs fake pendingPool |> shouldEqual (Some postponed, None)
            kickoffs fake settledPool |> shouldEqual (Some postponed, Some postponed)
            fake.TakeChanges() |> List.length |> shouldEqual 2

            let! redelivered = deliver fake

            redelivered.BatchItemFailures |> shouldBeEmpty
            fake.TakeChanges() |> shouldEqual []
        }
