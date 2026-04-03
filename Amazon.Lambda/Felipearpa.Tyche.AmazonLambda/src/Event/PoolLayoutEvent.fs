namespace Felipearpa.Tyche.AmazonLambda.Event

#nowarn "3536"

open System.Threading.Tasks
open Amazon.DynamoDBv2
open Amazon.Lambda.Core
open Amazon.Lambda.DynamoDBEvents
open Felipearpa.Core
open Felipearpa.Core.Json
open Felipearpa.Tyche.Pool.Application
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Infrastructure
open Microsoft.Extensions.DependencyInjection
open Microsoft.Extensions.Logging

type PoolLayoutEvent(configureServices: IServiceCollection -> unit) =

    let buildServiceProvider () =
        let services = ServiceCollection()

        services
            .AddAWSService<IAmazonDynamoDB>()
            .AddLogging(fun builder -> builder.AddLambdaLogger() |> ignore)
            .AddSingleton<ISerializer, JsonSerializer>()
            .AddSingleton<IPoolGamblerScoreRepository, PoolGamblerScoreDynamoDbRepository>()
            .AddSingleton<ComputeBetsCommand>()
        |> ignore

        configureServices services

        services.BuildServiceProvider()

    let serviceProvider = buildServiceProvider ()

    new() = PoolLayoutEvent(fun _ -> ())

    member this.OnPoolLayoutChangeAsync(event: DynamoDBEvent, _: ILambdaContext) : Task =
        (async {
            use scope = serviceProvider.CreateScope()

            let computeBetsCommand =
                scope.ServiceProvider.GetRequiredService<ComputeBetsCommand>()

            do!
                PoolLayoutEventFilter.extractUpdatedPoolLayouts event
                |> Seq.iterAsync computeBetsCommand.ExecuteAsync

            return ()
         }
         |> Async.StartAsTask
        :> Task)
