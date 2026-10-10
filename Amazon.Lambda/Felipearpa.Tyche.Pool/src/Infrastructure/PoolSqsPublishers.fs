namespace Felipearpa.Tyche.Pool.Infrastructure

#nowarn "3536"

open System
open Amazon.SQS
open Amazon.SQS.Model
open Felipearpa.Tyche.Pool.Domain

module private SqsBatch =

    [<Literal>]
    let maxEntries = 10

    /// Sends entries in batches and raises unless SQS accepted every entry.
    let sendAll (client: IAmazonSQS) (queueUrl: string) (entries: SendMessageBatchRequestEntry list) =
        async {
            for chunk in entries |> List.chunkBySize maxEntries do
                let request = SendMessageBatchRequest(QueueUrl = queueUrl, Entries = ResizeArray chunk)
                let! response = client.SendMessageBatchAsync request |> Async.AwaitTask

                match response.Failed |> Option.ofObj |> Option.map List.ofSeq |> Option.defaultValue [] with
                | [] -> ()
                | failed ->
                    let reasons = failed |> List.map (fun entry -> $"{entry.Id}: {entry.Code} {entry.Message}") |> String.concat "; "
                    return raise (InvalidOperationException $"SQS rejected {failed.Length} message(s): {reasons}")
        }

/// Publishes pool commands to the per-pool FIFO queue. The pool ID is the message group, so
/// a pool's commands are consumed in order; the operation ID is the deduplication ID, which
/// only suppresses resends within SQS's window and is not the idempotency contract.
type PoolCommandSqsPublisher(client: IAmazonSQS, queueUrl: string) =

    let send (deduplicationId: PoolCommand -> string) (commands: PoolCommand list) =
        commands
        |> List.mapi (fun index command ->
            SendMessageBatchRequestEntry(
                Id = string index,
                MessageBody = PoolMessageJson.encodeCommand command,
                MessageGroupId = command.PoolId.Value,
                MessageDeduplicationId = deduplicationId command
            ))
        |> SqsBatch.sendAll client queueUrl

    interface IPoolCommandPublisher with
        member this.PublishAsync(commands) = send PoolMessageJson.deduplicationId commands

        member this.RepublishAsync(commands) =
            send (fun _ -> Guid.NewGuid().ToString("N")) commands

/// Publishes version-guarded maintenance jobs to the standard projection queue.
type PoolMaintenanceSqsPublisher(client: IAmazonSQS, queueUrl: string) =

    interface IPoolMaintenancePublisher with
        member this.PublishAsync(jobs) =
            jobs
            |> List.mapi (fun index job -> SendMessageBatchRequestEntry(Id = string index, MessageBody = PoolMessageJson.encodeJob job))
            |> SqsBatch.sendAll client queueUrl
