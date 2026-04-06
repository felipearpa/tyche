namespace Felipearpa.Tyche.Pool.Infrastructure

#nowarn "3536"

open Amazon.SQS
open Amazon.SQS.Model
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

type UpdatePositionsSqsPublisher(sqsClient: IAmazonSQS, queueUrl: string) =

    interface IUpdatePositionsPublisher with

        member this.Publish(poolId, matchId) =
            let poolIdValue = poolId |> Ulid.value
            let matchIdValue = matchId |> Ulid.value

            let request = SendMessageRequest()
            request.QueueUrl <- queueUrl
            request.MessageBody <- $"{poolIdValue}|{matchIdValue}"
            request.MessageGroupId <- poolIdValue
            request.MessageDeduplicationId <- $"{poolIdValue}_{matchIdValue}"

            async { do! sqsClient.SendMessageAsync(request) |> Async.AwaitTask |> Async.Ignore }
