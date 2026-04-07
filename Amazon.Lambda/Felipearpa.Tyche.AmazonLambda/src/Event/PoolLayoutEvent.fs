namespace Felipearpa.Tyche.AmazonLambda.Event

#nowarn "3536"

open System
open System.Threading.Tasks
open Amazon.DynamoDBv2
open Amazon.Lambda.Core
open Amazon.Lambda.DynamoDBEvents
open Amazon.SQS
open Felipearpa.Core
open Felipearpa.Core.Json
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.Pool.Application
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Infrastructure
open Microsoft.Extensions.DependencyInjection
open Microsoft.Extensions.Logging

type PoolLayoutEvent(configureServices: IServiceCollection -> unit) =

    let buildServiceProvider () =
        let services = ServiceCollection()

        let queueUrl = Environment.GetEnvironmentVariable("UPDATE_POSITIONS_QUEUE_URL")

        services
            .AddAWSService<IAmazonDynamoDB>()
            .AddAWSService<IAmazonSQS>()
            .AddLogging(fun builder -> builder.AddLambdaLogger() |> ignore)
            .AddSingleton<ISerializer, JsonSerializer>()
            .AddSingleton<IKeySerializer, DynamoDbKeySerializer>()
            .AddSingleton<IUpdatePositionsPublisher>(
                System.Func<System.IServiceProvider, IUpdatePositionsPublisher>(fun sp ->
                    UpdatePositionsSqsPublisher(sp.GetRequiredService<IAmazonSQS>(), queueUrl))
            )
            .AddSingleton<IPoolGamblerScoreRepository, PoolGamblerScoreDynamoDbRepository>()
            .AddSingleton<ComputeBets>()
        |> ignore

        configureServices services

        services.BuildServiceProvider()

    let serviceProvider = buildServiceProvider ()

    new() = PoolLayoutEvent(fun _ -> ())

    member this.OnPoolLayoutChangeAsync(event: DynamoDBEvent, _: ILambdaContext) : Task =
        (async {
            use scope = serviceProvider.CreateScope()

            let computeBets = scope.ServiceProvider.GetRequiredService<ComputeBets>()

            do!
                PoolLayoutEventFilter.extractUpdatedPoolLayouts event
                |> Seq.iterAsync computeBets.ExecuteAsync

            return ()
         }
         |> Async.StartAsTask
        :> Task)
