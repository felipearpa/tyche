namespace Felipearpa.Tyche.AmazonLambda.Event.Tests

#nowarn "3536"

open System.Collections.Generic
open Amazon.Lambda.DynamoDBEvents
open Amazon.Lambda.TestUtilities
open Felipearpa.Tyche.AmazonLambda.Event
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type
open FsUnitTyped
open Microsoft.Extensions.DependencyInjection
open Xunit

/// How the membership stream handler acknowledges a join only after its commands are enqueued.
module MembershipEventTest =

    let private poolId = "01K1PX1TX2NM1HG851S1V0P001"
    let private gamblerId = "01K1PX1TX2NM1HG851S1V0GD01"

    let private join () =
        let stream = DynamoDBEvent.StreamRecord()
        stream.Keys <- Dictionary(dict [ "pk", DynamoDBEvent.AttributeValue(S = $"POOL#{poolId}"); "sk", DynamoDBEvent.AttributeValue(S = $"GAMBLER#{gamblerId}") ])
        stream.NewImage <- Dictionary(dict [ "membershipGeneration", DynamoDBEvent.AttributeValue(S = "G2") ])
        stream.SequenceNumber <- "100"
        let record = DynamoDBEvent.DynamodbStreamRecord()
        record.EventName <- "INSERT"
        record.EventID <- "event-1"
        record.Dynamodb <- stream
        DynamoDBEvent(Records = List([ record ]))

    let private handlerWith (publisher: IPoolCommandPublisher) =
        let maintenance =
            { new IPoolMaintenancePublisher with
                member _.PublishAsync(_) = async { return () } }

        MembershipEvent(fun services ->
            services
                .AddSingleton<IPoolCommandPublisher>(publisher)
                .AddSingleton<IPoolMaintenancePublisher>(maintenance)
            |> ignore)

    [<Fact>]
    let ``a join is acknowledged after its standings and restore commands are enqueued`` () =
        async {
            let published = ref []

            let publisher =
                { new IPoolCommandPublisher with
                    member _.PublishAsync(commands) = async { published.Value <- published.Value @ commands }
                    member _.RepublishAsync(_) = failwith "not used" }

            let! response = (handlerWith publisher).OnMembershipChangeAsync(join (), TestLambdaContext()) |> Async.AwaitTask

            response.BatchItemFailures |> shouldBeEmpty

            published.Value |> List.map _.OperationId
            |> shouldEqual [ "STANDINGS#event-1"; $"RESTORE#{gamblerId}#G2" ]

            published.Value |> List.last |> _.Kind
            |> shouldEqual (RestoreRejoinHistory(Ulid.newOf gamblerId, "G2"))
        }

    [<Fact>]
    let ``a join whose restore cannot be enqueued is reported as failed for retry`` () =
        async {
            let publisher =
                { new IPoolCommandPublisher with
                    member _.PublishAsync(_) = async { return failwith "SQS unavailable" }
                    member _.RepublishAsync(_) = failwith "not used" }

            let! response = (handlerWith publisher).OnMembershipChangeAsync(join (), TestLambdaContext()) |> Async.AwaitTask

            response.BatchItemFailures |> Seq.map _.ItemIdentifier |> List.ofSeq |> shouldEqual [ "100" ]
        }
