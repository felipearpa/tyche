namespace Felipearpa.Tyche.AmazonLambda.Event

#nowarn "3536"

open System.Threading.Tasks
open Amazon.Lambda.Core
open Amazon.Lambda.DynamoDBEvents
open Felipearpa.Tyche.Pool.Application
open Microsoft.Extensions.DependencyInjection
open Microsoft.Extensions.Logging

/// PoolLayout stream handler. Records are handled in source order: an added match is
/// materialized (and its settlement requested if it already has a result), and an accepted
/// result or correction is dispatched as durable per-pool commands before any settlement.
type PoolLayoutEvent(configureServices: IServiceCollection -> unit) =

    let serviceProvider = PoolServices.build configureServices

    new() = PoolLayoutEvent(fun _ -> ())

    member this.OnPoolLayoutChangeAsync(event: DynamoDBEvent, _: ILambdaContext) : Task<StreamsEventResponse> =
        let dispatch = serviceProvider.GetRequiredService<DispatchMatchResult>()
        let fanOut = serviceProvider.GetRequiredService<FanOutMatchToGamblers>()
        let logger = serviceProvider.GetRequiredService<ILogger<PoolLayoutEvent>>()

        let handle (record: DynamoDBEvent.DynamodbStreamRecord) =
            async {
                match PoolLayoutEventFilter.tryResultChange record with
                | Some(matchId, result) ->
                    let! pools = dispatch.ExecuteAsync(matchId, result)

                    OperationMetrics.emit
                        "PoolLayoutEvent"
                        [ OperationMetrics.count "ResultCommandsDispatched" pools ]
                        [ "matchId", matchId.Value; "resultVersion", string result.Version ]
                | None -> ()

                match PoolLayoutEventFilter.tryInsertedMatch record with
                | Some input -> do! fanOut.ExecuteAsync(input, record.EventID)
                | None -> ()
            }

        StreamBatch.processInOrder "PoolLayoutEvent" logger handle event |> Async.StartAsTask
