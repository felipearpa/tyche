namespace Felipearpa.Tyche.AmazonLambda.Event

#nowarn "3536"

open System
open System.Threading.Tasks
open Amazon.Lambda.Core
open Amazon.Lambda.SQSEvents
open Amazon.SQS
open Amazon.SQS.Model
open Felipearpa.Tyche.Pool.Application
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Infrastructure
open Microsoft.Extensions.DependencyInjection
open Microsoft.Extensions.Logging

/// Consumer of the per-pool FIFO command queue. A command's message stays unacknowledged
/// until its operation completes: a deadline yield makes it visible again at once, a yield to
/// a lease another worker holds keeps it invisible for the rest of that lease, a failure
/// leaves it for the queue's retry and redrive policy, and completion deletes it.
type PoolOperationEvent(configureServices: IServiceCollection -> unit) =

    /// Time kept back from the invocation deadline to persist the checkpoint and return.
    let deadlineMargin = TimeSpan.FromSeconds 45.0

    let serviceProvider = PoolServices.build configureServices

    let toCommand (message: SQSEvent.SQSMessage) =
        match PoolMessageJson.decodePoolMessage message.Body with
        | PoolMessageJson.Command command -> command
        // Messages written by the previous position worker carry only the pool and match;
        // they become a match reconciliation named after the message.
        | PoolMessageJson.LegacyPositions(poolId, matchId) ->
            PoolCommand.reconcileMatch poolId matchId $"LEGACY#{message.MessageId}"

    let outcomeMetric (report: PoolCommandReport) =
        match report.Outcome with
        | PoolCommandOutcome.OperationCompleted -> "OperationCompleted"
        | PoolCommandOutcome.AlreadyCompleted -> "CommandAlreadyCompleted"
        | PoolCommandOutcome.Yielded -> "OperationYielded"
        | PoolCommandOutcome.DeferredWhileBlocked -> "CommandDeferred"
        | PoolCommandOutcome.Blocked -> "OperationBlocked"
        | PoolCommandOutcome.PoolNotFound -> "CommandForMissingPool"

    let emit (command: PoolCommand) (report: PoolCommandReport) =
        let age =
            report.ActiveSince
            |> Option.map (fun since -> OperationMetrics.seconds "ActiveOperationAge" (DateTime.UtcNow - since).TotalSeconds)
            |> Option.toList

        OperationMetrics.emit
            "PoolOperationEvent"
            ([ OperationMetrics.count (outcomeMetric report) 1
               OperationMetrics.count "SettledDetails" report.Settled
               OperationMetrics.count "UnresolvedDetails" report.Unresolved ]
             @ age)
            [ "poolId", command.PoolId.Value
              "operationId", command.OperationId
              "activeOperationId", report.ActiveOperationId |> Option.defaultValue "" ]

    new() = PoolOperationEvent(fun _ -> ())

    member this.OnPoolCommandAsync(event: SQSEvent, context: ILambdaContext) : Task<SQSBatchResponse> =
        let processCommand = serviceProvider.GetRequiredService<ProcessPoolCommand>()
        let sqs = serviceProvider.GetRequiredService<IAmazonSQS>()
        let logger = serviceProvider.GetRequiredService<ILogger<PoolOperationEvent>>()
        let deadline = DateTime.UtcNow + context.RemainingTime - deadlineMargin

        let makeVisibleIn (message: SQSEvent.SQSMessage) (delay: TimeSpan) =
            async {
                let request =
                    ChangeMessageVisibilityRequest(
                        QueueUrl = PoolServices.commandQueueUrl (),
                        ReceiptHandle = message.ReceiptHandle,
                        VisibilityTimeout = int (ceil (max TimeSpan.Zero delay).TotalSeconds)
                    )

                let! _ = sqs.ChangeMessageVisibilityAsync request |> Async.AwaitTask
                return ()
            }

        let handle (message: SQSEvent.SQSMessage) =
            async {
                let command = toCommand message
                let! report = processCommand.ExecuteAsync(command, deadline)
                emit command report

                match report.Outcome with
                | PoolCommandOutcome.Yielded ->
                    // A deadline yield comes back at once; a yield to a live lease waits it out.
                    do! makeVisibleIn message (report.RetryAfter |> Option.defaultValue TimeSpan.Zero)
                    return false
                | PoolCommandOutcome.Blocked ->
                    logger.LogError(
                        "Pool {PoolId} is blocked on operation {OperationId}; see docs/pool-settlement.md",
                        command.PoolId.Value,
                        report.ActiveOperationId |> Option.defaultValue command.OperationId
                    )

                    return false
                | _ -> return true
            }

        // Messages of one group must not be acknowledged past a failed one.
        let rec loop (messages: SQSEvent.SQSMessage list) (failed: SQSEvent.SQSMessage list) =
            async {
                match messages with
                | [] -> return failed
                | message :: rest when not failed.IsEmpty -> return! loop rest (failed @ [ message ])
                | message :: rest ->
                    let! outcome = handle message |> Async.Catch

                    match outcome with
                    | Choice1Of2 true -> return! loop rest failed
                    | Choice1Of2 false -> return! loop rest [ message ]
                    | Choice2Of2 error ->
                        logger.LogError(error, "Pool command {MessageId} failed", message.MessageId)
                        OperationMetrics.emit "PoolOperationEvent" [ OperationMetrics.count "CommandFailures" 1 ] [ "messageId", message.MessageId ]
                        return! loop rest [ message ]
            }

        async {
            let! failed = loop (event.Records |> Option.ofObj |> Option.map List.ofSeq |> Option.defaultValue []) []

            return
                SQSBatchResponse(
                    BatchItemFailures =
                        ResizeArray(failed |> List.map (fun message -> SQSBatchResponse.BatchItemFailure(ItemIdentifier = message.MessageId)))
                )
        }
        |> Async.StartAsTask
