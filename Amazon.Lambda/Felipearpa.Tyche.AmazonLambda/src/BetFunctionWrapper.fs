namespace Felipearpa.Tyche.AmazonLambda

#nowarn "3536"

open System.Threading.Tasks
open Amazon.DynamoDBv2
open Amazon.Lambda.APIGatewayEvents
open Amazon.Lambda.Core
open Felipearpa.Core
open Felipearpa.Core.Json
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.Function.BetFunction
open Felipearpa.Tyche.Function.Request
open Felipearpa.Tyche.Pool.Application
open Felipearpa.Tyche.Pool.Data
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Infrastructure
open Microsoft.Extensions.DependencyInjection

type BetFunctionWrapper(configureServices: IServiceCollection -> unit) =

    let buildServiceProvider () =
        let services = ServiceCollection()

        services
            .AddAWSService<IAmazonDynamoDB>()
            .AddSingleton<ISerializer, JsonSerializer>()
            .AddSingleton<IKeySerializer, DynamoDbKeySerializer>()
            .AddScoped<IPoolGamblerScoreRepository, PoolGamblerScoreDynamoDbRepository>()
            .AddScoped<IPoolGamblerBetRepository, PoolGamblerBetDynamoDbRepository>()
            .AddScoped<IPoolRepository, PoolDynamoDbRepository>()
            .AddScoped<GetPoolGamblerScoresByGamblerQuery>()
            .AddScoped<GetPoolGamblerScoresByPoolQuery>()
            .AddScoped<GetPendingPoolGamblerBetsQuery>()
            .AddScoped<GetFinishedPoolGamblerBetsQuery>()
            .AddScoped<GetPoolGamblerScoreByIdQuery>()
            .AddScoped<GetPoolByIdQuery>()
            .AddScoped<BetCommand>()
            .AddScoped<CreatePoolCommand>()
            .AddScoped<JoinPoolCommand>()
        |> ignore

        configureServices services

        services.BuildServiceProvider()

    let serviceProvider = buildServiceProvider ()

    new() = BetFunctionWrapper(fun _ -> ())

    // PATCH /bets
    member this.Bet
        (request: APIGatewayHttpApiV2ProxyRequest, _: ILambdaContext)
        : APIGatewayHttpApiV2ProxyResponse Task =
        async {
            use scope = serviceProvider.CreateScope()

            let betRequestResult = tryGetOrError<BetRequest> request.Body

            match betRequestResult with
            | Error error -> return [ error ] |> BadRequestResponseFactory.create
            | Ok betPoolRequest ->
                let! response = bet betPoolRequest (scope.ServiceProvider.GetService<BetCommand>())
                return! response.ToAmazonProxyResponse()
        }
        |> Async.StartAsTask
