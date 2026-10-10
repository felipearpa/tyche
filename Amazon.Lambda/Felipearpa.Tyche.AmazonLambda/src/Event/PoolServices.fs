namespace Felipearpa.Tyche.AmazonLambda.Event

#nowarn "3536"

open System
open Amazon.DynamoDBv2
open Amazon.SQS
open Felipearpa.Core
open Felipearpa.Core.Json
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.Pool.Application
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Infrastructure
open Felipearpa.Tyche.PoolLayout.Application
open Felipearpa.Tyche.PoolLayout.Domain
open Felipearpa.Tyche.PoolLayout.Infrastructure
open Microsoft.Extensions.DependencyInjection
open Microsoft.Extensions.Logging

/// Composition shared by the pool stream, command, and maintenance handlers.
module PoolServices =

    /// The per-pool FIFO queue for ordered pool commands.
    let commandQueueUrl () = Environment.GetEnvironmentVariable "POOL_COMMAND_QUEUE_URL"

    /// The standard queue for version-guarded maintenance jobs.
    let maintenanceQueueUrl () = Environment.GetEnvironmentVariable "POOL_MAINTENANCE_QUEUE_URL"

    let private clock: unit -> DateTime = fun () -> DateTime.UtcNow

    let private factory<'T> (create: IServiceProvider -> 'T) = Func<IServiceProvider, 'T>(create)

    let register (services: IServiceCollection) =
        services
            .AddAWSService<IAmazonDynamoDB>()
            .AddAWSService<IAmazonSQS>()
            .AddLogging(fun builder ->
                builder.AddLambdaLogger(LambdaLoggerOptions(IncludeException = true)) |> ignore)
            .AddSingleton<ISerializer, JsonSerializer>()
            .AddSingleton<IKeySerializer, DynamoDbKeySerializer>()
            .AddSingleton<unit -> DateTime>(clock)
            .AddSingleton<PoolOperationSettings>(PoolOperationSettings.Default)
            .AddSingleton<PoolSettlementDynamoDbRepository>(
                factory (fun sp -> PoolSettlementDynamoDbRepository(sp.GetRequiredService<IAmazonDynamoDB>()))
            )
            .AddSingleton<IPoolSettlementRepository>(
                factory (fun sp -> sp.GetRequiredService<PoolSettlementDynamoDbRepository>() :> IPoolSettlementRepository)
            )
            .AddSingleton<IPoolProjectionRepository>(
                factory (fun sp -> sp.GetRequiredService<PoolSettlementDynamoDbRepository>() :> IPoolProjectionRepository)
            )
            .AddSingleton<IPoolGamblerBetRepository, PoolGamblerBetDynamoDbRepository>()
            .AddSingleton<IPoolRepository, PoolDynamoDbRepository>()
            .AddSingleton<IPoolLayoutRepository, PoolLayoutDynamoDbRepository>()
            .AddSingleton<IPoolLayoutMatchReader, PoolLayoutMatchReader>()
            .AddSingleton<IPoolCommandPublisher>(
                factory (fun sp ->
                    PoolCommandSqsPublisher(sp.GetRequiredService<IAmazonSQS>(), commandQueueUrl ()) :> IPoolCommandPublisher)
            )
            .AddSingleton<IPoolMaintenancePublisher>(
                factory (fun sp ->
                    PoolMaintenanceSqsPublisher(sp.GetRequiredService<IAmazonSQS>(), maintenanceQueueUrl ())
                    :> IPoolMaintenancePublisher)
            )
            .AddSingleton<ProcessPoolCommand>()
            .AddSingleton<DispatchMatchResult>()
            .AddSingleton<FanOutMatchToGamblers>()
            .AddSingleton<MaterializeJoinedGambler>()
            .AddSingleton<CleanUpRemovedMembership>()
            .AddSingleton<ReactToMembershipChanges>()
            .AddSingleton<PropagateMembershipTotal>()
            .AddSingleton<InitializePoolIdentities>()
            .AddSingleton<BackfillPoolProjection>()
            .AddSingleton<VerifyPoolRankingReadiness>()
            .AddSingleton<ReconcilePool>()
        |> ignore

    let build (configureServices: IServiceCollection -> unit) =
        let services = ServiceCollection()
        register services
        configureServices services
        services.BuildServiceProvider()
