namespace Felipearpa.Tyche.AmazonLambda

#nowarn "3536"

open System.Threading.Tasks
open Amazon.DynamoDBv2
open Amazon.Lambda.APIGatewayEvents
open Amazon.Lambda.Core
open Felipearpa.Core
open Felipearpa.Core.Json
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.AmazonLambda.StringTransformer
open Felipearpa.Tyche.Function.GamblerFunction
open Felipearpa.Tyche.Pool.Application
open Felipearpa.Tyche.Pool.Data
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Infrastructure
open Microsoft.Extensions.DependencyInjection

type GamblerFunctionWrapper(configureServices: IServiceCollection -> unit) =

    [<Literal>]
    let poolIdParameter = "poolId"

    [<Literal>]
    let gamblerIdParameter = "gamblerId"

    [<Literal>]
    let nextParameter = "next"

    [<Literal>]
    let searchTextParameter = "searchText"

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

    new() = GamblerFunctionWrapper(fun _ -> ())

    // URL: /gamblers/{gamblerId}/pools?next={next}
    member this.GetPoolsByGamblerId
        (request: APIGatewayHttpApiV2ProxyRequest, _: ILambdaContext)
        : APIGatewayHttpApiV2ProxyResponse Task =
        async {
            use scope = serviceProvider.CreateScope()

            let gamblerIdResult =
                request.PathParameters
                |> Option.ofObj
                |> Option.defaultValue Map.empty
                |> tryGetStringParamOrError gamblerIdParameter

            let maybeNext =
                request.QueryStringParameters
                |> Option.ofObj
                |> Option.defaultValue Map.empty
                |> tryGetStringParamOrNone nextParameter

            match gamblerIdResult, maybeNext with
            | Ok gamblerId, next ->
                let! response =
                    getPoolsByGamblerId
                        gamblerId
                        (next |> Option.bind noneIfEmpty)
                        (scope.ServiceProvider.GetService<GetPoolGamblerScoresByGamblerQuery>())

                return! response.ToAmazonProxyResponse()
            | _ ->
                let errors = [ toErrorOption gamblerIdResult ] |> List.choose id

                return errors |> BadRequestResponseFactory.create
        }
        |> Async.StartAsTask
