namespace Felipearpa.Tyche.Verification

open System
open System.Diagnostics
open Amazon.DynamoDBStreams
open Amazon.Lambda.DynamoDBEvents
open Amazon.Lambda.TestUtilities
open Felipearpa.Tyche.AmazonLambda.Event

/// Runs the production stream and queue handlers over the isolated resources until the
/// whole pipeline is idle.
type Pipeline
    (
        streams: AmazonDynamoDBStreamsClient,
        poolStreamArn: string,
        layoutStreamArn: string,
        workers: QueueWorkers,
        startAtLatest: bool
    ) =

    let poolPump = StreamPump(streams, poolStreamArn, startAtLatest)
    let layoutPump = StreamPump(streams, layoutStreamArn, startAtLatest)
    let poolEvent = PoolEvent()
    let membershipEvent = MembershipEvent()
    let layoutEvent = PoolLayoutEvent()
    let mutable streamFailures = 0

    /// Delivers records in batches of 100, retrying from the first reported failure as the
    /// event source mapping does, up to its retry limit.
    let deliver (handler: DynamoDBEvent -> Async<StreamsEventResponse>) (records: DynamoDBEvent.DynamodbStreamRecord list) =
        let rec attempt (remaining: DynamoDBEvent.DynamodbStreamRecord list) tries =
            async {
                match remaining with
                | [] -> ()
                | _ ->
                    let batch = remaining |> List.truncate 100
                    let! response = handler (DynamoDBEvent(Records = Collections.Generic.List(batch)))

                    match response.BatchItemFailures |> Seq.tryHead with
                    | None -> return! attempt (remaining |> List.skip batch.Length) 0
                    | Some failure when tries < 10 ->
                        streamFailures <- streamFailures + 1
                        let failedAt = remaining |> List.findIndex (fun r -> r.Dynamodb.SequenceNumber = failure.ItemIdentifier)
                        do! Async.Sleep 500
                        return! attempt (remaining |> List.skip failedAt) (tries + 1)
                    | Some _ -> return failwith "Stream records exhausted their retries"
            }

        attempt records 0

    member _.Workers = workers
    member _.StreamFailures = streamFailures

    member _.PumpStreamsOnceAsync() =
        async {
            let! layoutRecords = layoutPump.PollAsync()
            do! deliver (fun event -> layoutEvent.OnPoolLayoutChangeAsync(event, TestLambdaContext()) |> Async.AwaitTask) layoutRecords
            let! poolRecords = poolPump.PollAsync()
            do! deliver (fun event -> poolEvent.OnPoolChangeAsync(event, TestLambdaContext()) |> Async.AwaitTask) poolRecords

            do!
                deliver
                    (fun event -> membershipEvent.OnMembershipChangeAsync(event, TestLambdaContext()) |> Async.AwaitTask)
                    poolRecords

            return layoutRecords.Length + poolRecords.Length
        }

    /// Pumps streams and queues until several consecutive rounds find nothing to do.
    member this.RunUntilQuiescentAsync(timeout: TimeSpan, ?commandConcurrency: int, ?maintenanceConcurrency: int) =
        let commandWorkers = defaultArg commandConcurrency 1
        let maintenanceWorkers = defaultArg maintenanceConcurrency 2
        let watch = Stopwatch.StartNew()

        let rec loop idleRounds =
            async {
                if watch.Elapsed > timeout then
                    return failwith $"Pipeline did not settle within {timeout}"
                elif idleRounds >= 3 then
                    return watch.Elapsed
                else
                    let! streamed = this.PumpStreamsOnceAsync()

                    let! commands =
                        Async.Parallel(List.replicate commandWorkers (workers.PollCommandsOnceAsync 1))

                    let! jobs =
                        Async.Parallel(List.replicate maintenanceWorkers (workers.PollMaintenanceOnceAsync 1))

                    let busy = streamed + Array.sum commands + Array.sum jobs

                    if busy > 0 then
                        return! loop 0
                    else
                        // Messages waiting out a visibility timeout still count as pending work.
                        let! pending = workers.PendingAsync()

                        if pending > 0 then
                            do! Async.Sleep 1000
                            return! loop 0
                        else
                            return! loop (idleRounds + 1)
            }

        loop 0
