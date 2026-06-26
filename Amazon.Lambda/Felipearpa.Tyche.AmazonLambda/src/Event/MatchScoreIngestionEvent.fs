namespace Felipearpa.Tyche.AmazonLambda.Event

#nowarn "3536"

open System
open System.Threading.Tasks
open Amazon.Lambda.Core
open Amazon.Lambda.DynamoDBEvents
open Amazon.Scheduler
open Felipearpa.Core
open Felipearpa.Tyche.MatchScoreIngestion.Domain
open Felipearpa.Tyche.MatchScoreIngestion.Infrastructure
open Microsoft.Extensions.DependencyInjection
open Microsoft.Extensions.Logging

type MatchScoreIngestionEvent(configureServices: IServiceCollection -> unit) =

    let buildServiceProvider () =
        let services = ServiceCollection()

        let targetFunctionArn = Environment.GetEnvironmentVariable("INGEST_MATCH_SCORE_FUNCTION_ARN")
        let schedulerRoleArn = Environment.GetEnvironmentVariable("MATCH_SCORE_SCHEDULER_ROLE_ARN")

        services
            .AddAWSService<IAmazonScheduler>()
            .AddLogging(fun builder -> builder.AddLambdaLogger() |> ignore)
            .AddSingleton<IMatchScoreScheduler>(
                System.Func<System.IServiceProvider, IMatchScoreScheduler>(fun sp ->
                    EventBridgeMatchScoreScheduler(
                        sp.GetRequiredService<IAmazonScheduler>(),
                        targetFunctionArn,
                        schedulerRoleArn
                    ))
            )
        |> ignore

        configureServices services

        services.BuildServiceProvider()

    let serviceProvider = buildServiceProvider ()

    new() = MatchScoreIngestionEvent(fun _ -> ())

    member this.OnMatchScoreIngestionChangeAsync(event: DynamoDBEvent, _: ILambdaContext) : Task =
        (async {
            use scope = serviceProvider.CreateScope()

            let scheduler = scope.ServiceProvider.GetRequiredService<IMatchScoreScheduler>()

            do!
                MatchScoreIngestionEventFilter.extractSchedulesToCreate event
                |> Seq.iterAsync (fun schedule -> scheduler.CreateScheduleAsync schedule)

            do!
                MatchScoreIngestionEventFilter.extractSchedulesToDelete event
                |> Seq.iterAsync (fun matchId -> scheduler.DeleteScheduleAsync matchId)

            return ()
         }
         |> Async.StartAsTask
        :> Task)
