namespace Felipearpa.Tyche.AmazonLambda.Event

#nowarn "3536"

open System.Threading.Tasks
open Amazon.Lambda.Core
open Amazon.Lambda.SQSEvents
open Felipearpa.Tyche.Pool.Application
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Infrastructure
open Microsoft.Extensions.DependencyInjection
open Microsoft.Extensions.Logging

/// Consumer of the standard maintenance queue: total propagation, identity initialization,
/// projection backfill, readiness checks, and reconciliation. Each job enqueues its own
/// continuation before it is acknowledged; failed jobs are retried and then retained in the
/// dead-letter queue.
type PoolMaintenanceEvent(configureServices: IServiceCollection -> unit) =

    let serviceProvider = PoolServices.build configureServices

    let run (message: SQSEvent.SQSMessage) =
        async {
            let job = PoolMessageJson.decodeJob message.Body

            match job with
            | PropagateTotal(poolId, gamblerId, cursor) ->
                let! updated =
                    serviceProvider.GetRequiredService<PropagateMembershipTotal>().ExecuteAsync(poolId, gamblerId, cursor)

                return [ OperationMetrics.count "TotalsPropagated" updated ], [ "poolId", poolId.Value; "gamblerId", gamblerId.Value ]
            | InitializeIdentities(poolId, cursor) ->
                let! report = serviceProvider.GetRequiredService<InitializePoolIdentities>().ExecuteAsync(poolId, cursor)

                return
                    [ OperationMetrics.count "IdentitiesBound" report.BoundDetails
                      OperationMetrics.count "IdentityMembersHeld" report.HeldMembers.Length ],
                    [ "poolId", poolId.Value
                      "heldMembers", report.HeldMembers |> List.map _.Value |> String.concat "," ]
            | BackfillProjection(poolId, cursor) ->
                let! initialized = serviceProvider.GetRequiredService<BackfillPoolProjection>().ExecuteAsync(poolId, cursor)
                return [ OperationMetrics.count "ProjectionsInitialized" initialized ], [ "poolId", poolId.Value ]
            | VerifyReadiness poolId ->
                let! report = serviceProvider.GetRequiredService<VerifyPoolRankingReadiness>().ExecuteAsync poolId

                return
                    [ OperationMetrics.count "ReadinessUnresolved" (report |> Option.map _.Unresolved |> Option.defaultValue 0) ],
                    [ "poolId", poolId.Value
                      "readiness", report |> Option.map (fun r -> string r.Readiness) |> Option.defaultValue "POOL_NOT_FOUND"
                      "checkedDetails", report |> Option.map (fun r -> string r.CheckedDetails) |> Option.defaultValue "0" ]
            | ReconcilePool(poolId, cursor) ->
                let! report =
                    serviceProvider.GetRequiredService<ReconcilePool>().ExecuteAsync(poolId, cursor, $"RECONCILE#{message.MessageId}")

                return
                    [ OperationMetrics.count "ReconciledMissingDetails" report.MaterializedDetails
                      OperationMetrics.count "ReconciledUnsettledMatches" report.UnsettledMatches.Length
                      OperationMetrics.count "ReconciledStaleProjections" report.StaleProjections
                      OperationMetrics.count "ReconciledStalePositions" report.StalePositions
                      OperationMetrics.count "ReconciledStaleGenerations" report.RetiredGenerations
                      OperationMetrics.count "ReconciledRestorations" report.RequestedRestorations
                      OperationMetrics.count "RestoreHeldDetails" report.HeldRetainedDetails ],
                    [ "poolId", poolId.Value ]
            | RetireMembership(poolId, gamblerId, generation) ->
                do! serviceProvider.GetRequiredService<CleanUpRemovedMembership>().ExecuteAsync(poolId, gamblerId, generation)
                return [ OperationMetrics.count "MembershipsRetired" 1 ], [ "poolId", poolId.Value; "gamblerId", gamblerId.Value ]
        }

    new() = PoolMaintenanceEvent(fun _ -> ())

    member this.OnPoolMaintenanceJobAsync(event: SQSEvent, _: ILambdaContext) : Task<SQSBatchResponse> =
        let logger = serviceProvider.GetRequiredService<ILogger<PoolMaintenanceEvent>>()

        async {
            let! results =
                event.Records
                |> Option.ofObj
                |> Option.map List.ofSeq
                |> Option.defaultValue []
                |> List.map (fun message ->
                    async {
                        let! outcome = run message |> Async.Catch

                        match outcome with
                        | Choice1Of2(metrics, properties) ->
                            OperationMetrics.emit "PoolMaintenanceEvent" metrics (("messageId", message.MessageId) :: properties)
                            return None
                        | Choice2Of2 error ->
                            logger.LogError(error, "Maintenance job {MessageId} failed: {Body}", message.MessageId, message.Body)
                            OperationMetrics.emit "PoolMaintenanceEvent" [ OperationMetrics.count "JobFailures" 1 ] [ "messageId", message.MessageId ]
                            return Some message.MessageId
                    })
                |> Async.Sequential

            return
                SQSBatchResponse(
                    BatchItemFailures =
                        ResizeArray(results |> Array.choose id |> Array.map (fun id -> SQSBatchResponse.BatchItemFailure(ItemIdentifier = id)))
                )
        }
        |> Async.StartAsTask
