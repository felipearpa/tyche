namespace Felipearpa.Tyche.AmazonLambda.Event

#nowarn "3536"

open System.Threading.Tasks
open Amazon.DynamoDBv2
open Amazon.Lambda.Core
open Amazon.Lambda.SQSEvents
open Felipearpa.Core
open Felipearpa.Core.Json
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.Pool.Application
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Infrastructure
open Felipearpa.Type
open Microsoft.Extensions.DependencyInjection
open Microsoft.Extensions.Logging

type UpdatePositionsEvent(configureServices: IServiceCollection -> unit) =

    let buildServiceProvider () =
        let services = ServiceCollection()

        services
            .AddAWSService<IAmazonDynamoDB>()
            .AddLogging(fun builder -> builder.AddLambdaLogger() |> ignore)
            .AddSingleton<ISerializer, JsonSerializer>()
            .AddSingleton<IKeySerializer, DynamoDbKeySerializer>()
            .AddSingleton<IPoolGamblerScoreRepository, PoolGamblerScoreDynamoDbRepository>()
            .AddScoped<UpdatePositionsCommand>()
        |> ignore

        configureServices services

        services.BuildServiceProvider()

    let serviceProvider = buildServiceProvider ()

    let parseMessage (body: string) =
        let parts = body.Split('|')
        (Ulid.newOf parts[0], Ulid.newOf parts[1])

    new() = UpdatePositionsEvent(fun _ -> ())

    member this.OnUpdatePositionsAsync(event: SQSEvent, _: ILambdaContext) : Task =
        (async {
            use scope = serviceProvider.CreateScope()

            let updatePositionsCommand =
                scope.ServiceProvider.GetRequiredService<UpdatePositionsCommand>()

            do!
                event.Records
                |> Seq.map (fun record ->
                    let (poolId, matchId) = parseMessage record.Body
                    updatePositionsCommand.ExecuteAsync(poolId, matchId))
                |> Seq.iterAsync id
         }
         |> Async.StartAsTask
        :> Task)
