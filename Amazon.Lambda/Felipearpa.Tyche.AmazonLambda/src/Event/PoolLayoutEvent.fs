namespace Felipearpa.Tyche.AmazonLambda.Event

#nowarn "3536"

open System.Threading.Tasks
open Amazon.Lambda.Core
open Amazon.Lambda.DynamoDBEvents
open Felipearpa.Tyche.Pool.Application
open Microsoft.Extensions.DependencyInjection
open Microsoft.Extensions.Logging

/// PoolLayout stream handler. Records are handled in source order: an added match is
/// materialized (and its settlement requested if it already has a result), an accepted
/// result or correction is dispatched as durable per-pool commands before any settlement, and
/// a rescheduled match's new kickoff is copied onto every gambler's detail.
type PoolLayoutEvent(configureServices: IServiceCollection -> unit) =

    let serviceProvider = PoolServices.build configureServices

    new() = PoolLayoutEvent(fun _ -> ())

    member this.OnPoolLayoutChangeAsync(event: DynamoDBEvent, _: ILambdaContext) : Task<StreamsEventResponse> =
        let dispatch = serviceProvider.GetRequiredService<DispatchMatchResult>()
        let fanOut = serviceProvider.GetRequiredService<FanOutMatchToGamblers>()
        let reschedule = serviceProvider.GetRequiredService<PropagateKickoffChange>()
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

                match PoolLayoutEventFilter.tryKickoffChange record with
                | Some(matchId, previous, kickoff) ->
                    let! written = reschedule.ExecuteAsync(matchId, previous, kickoff)

                    OperationMetrics.emit
                        "PoolLayoutEvent"
                        [ OperationMetrics.count "KickoffCopiesMoved" written ]
                        [ "matchId", matchId.Value; "previousKickoff", previous.ToString("o"); "kickoff", kickoff.ToString("o") ]
                | None -> ()

                match PoolLayoutEventFilter.tryInsertedMatch record with
                | Some input -> do! fanOut.ExecuteAsync(input, record.EventID)
                | None -> ()
            }

        StreamBatch.processInOrder "PoolLayoutEvent" logger handle event |> Async.StartAsTask
