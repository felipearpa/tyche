namespace Felipearpa.Tyche.AmazonLambda.Event.Tests

#nowarn "3536"

open System
open System.Collections.Generic
open System.Threading
open System.Threading.Tasks
open Amazon.Lambda.SQSEvents
open Amazon.Lambda.TestUtilities
open Amazon.SQS
open Amazon.SQS.Model
open Felipearpa.Tyche.AmazonLambda.Event
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Infrastructure
open Felipearpa.Type
open FsUnitTyped
open Microsoft.Extensions.DependencyInjection
open Moq
open Xunit

/// How the FIFO consumer turns a yielded command into queue visibility.
module PoolOperationEventTest =

    let private poolId = Ulid.newOf "01K1PX1TX2NM1HG851S1V0P001"
    let private matchId = Ulid.newOf "01K1PX1TX2NM1HG851S1V0M001"
    let private layoutId = Ulid.newOf "01K1PX1TX2NM1HG851S1V0Y001"

    let private result =
        { HomeTeamScore = 1
          AwayTeamScore = 0
          Version = 1 }

    let private command = PoolCommand.settleResult poolId matchId result

    /// A pool root whose operation for the command is held by another worker until the lease ends.
    let private rootHeldUntil (leaseExpiresAt: DateTime) =
        { PoolId = poolId
          PoolLayoutId = layoutId
          MembershipRevision = 0L
          OperationRevision = 1L
          LastCompletedOperationRevision = 0L
          RankingReadiness = None
          TrendKickoffDateTime = None
          ActiveOperation =
            Some
                { OperationId = command.OperationId
                  Revision = 1L
                  Target = MatchSettlement(matchId, result, OpensGroup(DateTime(2026, 6, 12, 17, 0, 0, DateTimeKind.Utc)))
                  Stage = Scoring
                  Cursor = None
                  CalculationRevision = 1
                  MembershipRevision = 0L
                  OwnerToken = "other-worker"
                  LeaseExpiresAt = leaseExpiresAt
                  AttemptsWithoutProgress = 0
                  Blocked = false
                  UnresolvedDetails = 0
                  RestoredDetails = 0
                  AdmittedAt = DateTime.UtcNow
                  LastProgressAt = DateTime.UtcNow } }

    let private repositoryWith (root: PoolSettlementRoot) =
        { new IPoolSettlementRepository with
            member _.GetRootAsync(_) = async { return Some root }
            member _.GetReceiptAsync(_, _) = failwith "not used"
            member _.AdmitAsync(_, _, _) = failwith "not used"
            member _.RecordCompletedAsync(_, _) = failwith "not used"
            member _.RecordDeferredAsync(_, _) = failwith "not used"
            member _.GetDeferredReceiptsAsync(_) = failwith "not used"
            member _.GetCompletedResultReceiptsAsync(_) = failwith "not used"
            member _.RecordTrendKickoffAsync(_, _) = failwith "not used"
            member _.ReplaceCheckpointAsync(_, _, _) = failwith "not used"
            member _.CompleteAsync(_, _, _, _) = failwith "not used"
            member _.GetMembershipPageAsync(_, _, _) = failwith "not used"
            member _.GetMembershipAsync(_, _) = failwith "not used"
            member _.GetBetSettlementStatesAsync(_, _, _) = failwith "not used"
            member _.GetBetSettlementStateAsync(_, _, _) = failwith "not used"
            member _.ApplySettlementAsync(_) = failwith "not used"
            member _.ApplyPositionAsync(_) = failwith "not used"
            member _.GetPoolsWithMatchPageAsync(_, _) = failwith "not used"
            member _.GetRetainedBetPageAsync(_, _, _, _) = failwith "not used"
            member _.GetRetainedBetAsync(_, _, _) = failwith "not used"
            member _.ApplyRebindingAsync(_) = failwith "not used"
            member _.HoldRetainedBetAsync(_) = failwith "not used" }

    let private layoutReader =
        { new IPoolLayoutMatchReader with
            member _.GetMatchAsync(_, _) = failwith "not used"
            member _.GetMatchesThroughVersionAsync(_, _, _) = failwith "not used" }

    let private commandPublisher =
        { new IPoolCommandPublisher with
            member _.PublishAsync(_) = failwith "not used"
            member _.RepublishAsync(_) = failwith "not used" }

    let private handlerOver (root: PoolSettlementRoot) (sqs: IAmazonSQS) =
        PoolOperationEvent(fun services ->
            services
                .AddSingleton<IAmazonSQS>(sqs)
                .AddSingleton<IPoolSettlementRepository>(repositoryWith root)
                .AddSingleton<IPoolLayoutMatchReader>(layoutReader)
                .AddSingleton<IPoolCommandPublisher>(commandPublisher)
            |> ignore)

    let private sqsRecording (requests: List<ChangeMessageVisibilityRequest>) =
        let sqs = Mock<IAmazonSQS>()

        sqs
            .Setup(fun s -> s.ChangeMessageVisibilityAsync(It.IsAny<ChangeMessageVisibilityRequest>(), It.IsAny<CancellationToken>()))
            .Returns(fun (request: ChangeMessageVisibilityRequest) (_: CancellationToken) ->
                requests.Add request
                Task.FromResult(ChangeMessageVisibilityResponse()))
        |> ignore

        sqs.Object

    let private event () =
        SQSEvent(
            Records =
                List(
                    [ SQSEvent.SQSMessage(
                          MessageId = "message-1",
                          ReceiptHandle = "handle-1",
                          Body = PoolMessageJson.encodeCommand command
                      ) ]
                )
        )

    [<Fact>]
    let ``a command that meets a live lease stays invisible for the rest of that lease`` () =
        async {
            let requests = List<ChangeMessageVisibilityRequest>()
            let handler = handlerOver (rootHeldUntil (DateTime.UtcNow.AddMinutes 4.0)) (sqsRecording requests)
            let context = TestLambdaContext(RemainingTime = TimeSpan.FromMinutes 5.0)

            let! response = handler.OnPoolCommandAsync(event (), context) |> Async.AwaitTask

            response.BatchItemFailures |> Seq.map _.ItemIdentifier |> List.ofSeq |> shouldEqual [ "message-1" ]
            requests.Count |> shouldEqual 1
            requests[0].ReceiptHandle |> shouldEqual "handle-1"
            requests[0].VisibilityTimeout.GetValueOrDefault() |> shouldBeGreaterThan 235
            requests[0].VisibilityTimeout.GetValueOrDefault() |> shouldBeSmallerThan 241
        }
