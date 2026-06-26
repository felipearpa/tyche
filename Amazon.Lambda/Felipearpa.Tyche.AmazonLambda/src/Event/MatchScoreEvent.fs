namespace Felipearpa.Tyche.AmazonLambda.Event

#nowarn "3536"

open System
open System.Net.Http
open System.Threading.Tasks
open Amazon.DynamoDBv2
open Amazon.Lambda.Core
open Amazon.SimpleSystemsManagement
open Amazon.SimpleSystemsManagement.Model
open Felipearpa.Core
open Felipearpa.Core.Json
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.MatchScoreIngestion.Application
open Felipearpa.Tyche.MatchScoreIngestion.Domain
open Felipearpa.Tyche.MatchScoreIngestion.Infrastructure
open Felipearpa.Tyche.PoolLayout.Domain
open Felipearpa.Tyche.PoolLayout.Infrastructure
open Felipearpa.Type
open Microsoft.Extensions.DependencyInjection
open Microsoft.Extensions.Logging

/// Payload delivered by the EventBridge schedule target.
[<CLIMutable>]
type IngestMatchScoreInput =
    { matchId: string
      poolLayoutId: string
      externalMatchId: string
      matchDateTime: string }

type MatchScoreEvent(configureServices: IServiceCollection -> unit) =

    let resolveToken () =
        let parameterName = Environment.GetEnvironmentVariable("FOOTBALL_DATA_TOKEN_PARAMETER")
        use ssm = new AmazonSimpleSystemsManagementClient()
        let request = GetParameterRequest(Name = parameterName, WithDecryption = true)
        let response = ssm.GetParameterAsync(request).GetAwaiter().GetResult()
        response.Parameter.Value

    let buildHttpClient () =
        let baseUrl =
            match Environment.GetEnvironmentVariable("FOOTBALL_DATA_BASE_URL") with
            | null
            | "" -> "https://api.football-data.org/"
            | value -> value

        let client = new HttpClient(BaseAddress = Uri(baseUrl))
        client.DefaultRequestHeaders.Add("X-Auth-Token", resolveToken ())
        client

    let httpClient = buildHttpClient ()

    let buildServiceProvider () =
        let services = ServiceCollection()

        services
            .AddAWSService<IAmazonDynamoDB>()
            .AddLogging(fun builder -> builder.AddLambdaLogger() |> ignore)
            .AddSingleton<HttpClient>(httpClient)
            .AddSingleton<ISerializer, JsonSerializer>()
            .AddSingleton<IKeySerializer, DynamoDbKeySerializer>()
            .AddScoped<IMatchResultProvider, FootballDataMatchResultProvider>()
            .AddScoped<IMatchScoreIngestionRepository, MatchScoreIngestionDynamoDbRepository>()
            .AddScoped<IPoolLayoutRepository, PoolLayoutDynamoDbRepository>()
            .AddScoped<IngestMatchScore>()
        |> ignore

        configureServices services

        services.BuildServiceProvider()

    let serviceProvider = buildServiceProvider ()

    new() = MatchScoreEvent(fun _ -> ())

    member this.IngestMatchScoreAsync(input: IngestMatchScoreInput, _: ILambdaContext) : Task =
        (async {
            use scope = serviceProvider.CreateScope()

            let ingestMatchScore = scope.ServiceProvider.GetRequiredService<IngestMatchScore>()

            do!
                ingestMatchScore.ExecuteAsync(
                    Ulid.newOf input.matchId,
                    Ulid.newOf input.poolLayoutId,
                    input.externalMatchId,
                    DateTime.Parse(input.matchDateTime)
                )

            return ()
         }
         |> Async.StartAsTask
        :> Task)
