namespace Felipearpa.Tyche.Verification

#nowarn "3536"

open System
open System.Collections.Generic
open System.Diagnostics
open Amazon.Lambda.SQSEvents
open Amazon.Lambda.TestUtilities
open Amazon.SQS
open Amazon.SQS.Model
open Felipearpa.Tyche.AmazonLambda.Event

/// Measurements of one delivery of a queue message to a handler.
type DeliveryRecord =
    { Queue: string
      MessageId: string
      ReceiveCount: int
      Succeeded: bool
      Elapsed: TimeSpan }

/// Polls the real queues and invokes the production SQS handlers the way the Lambda event
/// source mapping would: one FIFO message per invocation, partial batch responses, and
/// deletion only of acknowledged messages.
type QueueWorkers(sqs: IAmazonSQS, commandQueueUrl: string, maintenanceQueueUrl: string) =

    let operationEvent = PoolOperationEvent()
    let maintenanceEvent = PoolMaintenanceEvent()
    let deliveries = List<DeliveryRecord>()
    let gate = obj ()

    let toEventMessage (message: Message) =
        let converted = SQSEvent.SQSMessage()
        converted.MessageId <- message.MessageId
        converted.ReceiptHandle <- message.ReceiptHandle
        converted.Body <- message.Body
        converted.Attributes <- Dictionary(message.Attributes)
        converted

    let receive (queueUrl: string) (count: int) (waitSeconds: int) =
        async {
            let request =
                ReceiveMessageRequest(
                    QueueUrl = queueUrl,
                    MaxNumberOfMessages = count,
                    WaitTimeSeconds = waitSeconds,
                    MessageSystemAttributeNames = List([ "ApproximateReceiveCount"; "MessageGroupId" ])
                )

            let! response = sqs.ReceiveMessageAsync request |> Async.AwaitTask
            return response.Messages |> Option.ofObj |> Option.map List.ofSeq |> Option.defaultValue []
        }

    let delete queueUrl (message: Message) =
        sqs.DeleteMessageAsync(DeleteMessageRequest(QueueUrl = queueUrl, ReceiptHandle = message.ReceiptHandle))
        |> Async.AwaitTask
        |> Async.Ignore

    let releaseNow queueUrl (message: Message) =
        sqs.ChangeMessageVisibilityAsync(
            ChangeMessageVisibilityRequest(QueueUrl = queueUrl, ReceiptHandle = message.ReceiptHandle, VisibilityTimeout = 0)
        )
        |> Async.AwaitTask
        |> Async.Ignore

    let record queue (message: Message) succeeded elapsed =
        let receiveCount =
            match message.Attributes |> Option.ofObj with
            | Some attributes when attributes.ContainsKey "ApproximateReceiveCount" -> int attributes["ApproximateReceiveCount"]
            | _ -> 0

        lock gate (fun () ->
            deliveries.Add
                { Queue = queue
                  MessageId = message.MessageId
                  ReceiveCount = receiveCount
                  Succeeded = succeeded
                  Elapsed = elapsed })

    member val CommandRemainingTime = TimeSpan.FromMinutes 5.0 with get, set

    member _.Deliveries = lock gate (fun () -> List.ofSeq deliveries)

    /// One command invocation per received message. Returns the number of messages received.
    member this.PollCommandsOnceAsync(waitSeconds: int) =
        async {
            let! messages = receive commandQueueUrl 10 waitSeconds

            // Messages of one group arrive in order; a failure stops the rest of its group.
            let failedGroups = ref Set.empty<string>

            for message in messages do
                let group = message.Attributes |> Option.ofObj |> Option.bind (fun a -> match a.TryGetValue "MessageGroupId" with | true, g -> Some g | _ -> None) |> Option.defaultValue ""

                if failedGroups.Value.Contains group then
                    do! releaseNow commandQueueUrl message
                else
                    let context = TestLambdaContext(RemainingTime = this.CommandRemainingTime)
                    let event = SQSEvent(Records = List([ toEventMessage message ]))
                    let watch = Stopwatch.StartNew()
                    let! response = operationEvent.OnPoolCommandAsync(event, context) |> Async.AwaitTask
                    let succeeded = response.BatchItemFailures.Count = 0
                    record "commands" message succeeded watch.Elapsed

                    if succeeded then
                        do! delete commandQueueUrl message
                    else
                        failedGroups.Value <- failedGroups.Value.Add group

            return messages.Length
        }

    member this.PollMaintenanceOnceAsync(waitSeconds: int) =
        async {
            let! messages = receive maintenanceQueueUrl 5 waitSeconds

            if not messages.IsEmpty then
                let event = SQSEvent(Records = List(messages |> List.map toEventMessage))
                let watch = Stopwatch.StartNew()
                let! response = maintenanceEvent.OnPoolMaintenanceJobAsync(event, TestLambdaContext()) |> Async.AwaitTask
                let failed = response.BatchItemFailures |> Seq.map _.ItemIdentifier |> Set.ofSeq

                for message in messages do
                    let succeeded = not (failed.Contains message.MessageId)
                    record "maintenance" message succeeded watch.Elapsed

                    if succeeded then
                        do! delete maintenanceQueueUrl message

            return messages.Length
        }

    member _.QueueDepthAsync(queueUrl: string) =
        async {
            let! attributes =
                sqs.GetQueueAttributesAsync(
                    GetQueueAttributesRequest(
                        QueueUrl = queueUrl,
                        AttributeNames =
                            List([ "ApproximateNumberOfMessages"; "ApproximateNumberOfMessagesNotVisible"; "ApproximateNumberOfMessagesDelayed" ])
                    )
                )
                |> Async.AwaitTask

            return attributes.Attributes.Values |> Seq.sumBy int
        }

    member this.PendingAsync() =
        async {
            let! commands = this.QueueDepthAsync commandQueueUrl
            let! maintenance = this.QueueDepthAsync maintenanceQueueUrl
            return commands + maintenance
        }
