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
open Felipearpa.Tyche.Function.PoolFunction
open Felipearpa.Tyche.Pool.Application
open Felipearpa.Tyche.Pool.Data
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Infrastructure
open Microsoft.Extensions.DependencyInjection

type PoolFunctionWrapper(configureServices: IServiceCollection -> unit) =

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

    new() = PoolFunctionWrapper(fun _ -> ())

    // URL: /pools/{poolId}
    member this.GetPoolById
        (request: APIGatewayHttpApiV2ProxyRequest, _: ILambdaContext)
        : APIGatewayHttpApiV2ProxyResponse Task =
        async {
            use scope = serviceProvider.CreateScope()

            let poolIdResult =
                request.PathParameters
                |> Option.ofObj
                |> Option.defaultValue Map.empty
                |> tryGetStringParamOrError poolIdParameter

            match poolIdResult with
            | Error error -> return [ error ] |> BadRequestResponseFactory.create
            | Ok poolId ->
                let! response = getPoolById poolId (scope.ServiceProvider.GetService<GetPoolByIdQuery>())
                return! response.ToAmazonProxyResponse()
        }
        |> Async.StartAsTask

    // URL: /pools/{poolId}/gamblers?next={next}
    member this.GetGamblersByPoolId
        (request: APIGatewayHttpApiV2ProxyRequest, _: ILambdaContext)
        : APIGatewayHttpApiV2ProxyResponse Task =
        async {
            use scope = serviceProvider.CreateScope()

            let poolIdResult =
                request.PathParameters
                |> Option.ofObj
                |> Option.defaultValue Map.empty
                |> tryGetStringParamOrError poolIdParameter

            let maybeNext =
                request.QueryStringParameters
                |> Option.ofObj
                |> Option.defaultValue Map.empty
                |> tryGetStringParamOrNone nextParameter

            match poolIdResult, maybeNext with
            | Ok poolId, next ->
                let! response =
                    getGamblersByPoolId
                        poolId
                        (next |> Option.bind noneIfEmpty)
                        (scope.ServiceProvider.GetService<GetPoolGamblerScoresByPoolQuery>())

                return! response.ToAmazonProxyResponse()
            | _ ->
                let errors = [ toErrorOption poolIdResult ] |> List.choose id

                return errors |> BadRequestResponseFactory.create
        }
        |> Async.StartAsTask

    // URL: /pools/{poolId}/gamblers/{gamblerId}
    member this.GetPoolGamblerScoreById
        (request: APIGatewayHttpApiV2ProxyRequest, _: ILambdaContext)
        : APIGatewayHttpApiV2ProxyResponse Task =
        async {
            use scope = serviceProvider.CreateScope()

            let poolIdResult =
                request.PathParameters
                |> Option.ofObj
                |> Option.defaultValue Map.empty
                |> tryGetStringParamOrError poolIdParameter

            let gamblerIdResult =
                request.PathParameters
                |> Option.ofObj
                |> Option.defaultValue Map.empty
                |> tryGetStringParamOrError gamblerIdParameter

            match poolIdResult, gamblerIdResult with
            | Ok poolId, Ok gamblerId ->
                let! response =
                    getPoolGamblerScoreById
                        poolId
                        gamblerId
                        (scope.ServiceProvider.GetService<GetPoolGamblerScoreByIdQuery>())

                return! response.ToAmazonProxyResponse()
            | _ ->
                let errors =
                    [ toErrorOption poolIdResult; toErrorOption gamblerIdResult ] |> List.choose id

                return errors |> BadRequestResponseFactory.create
        }
        |> Async.StartAsTask

    // URL: /pools/{poolId}/gamblers/{gamblerId}/bets/pending
    member this.GetPendingBets
        (request: APIGatewayHttpApiV2ProxyRequest, _: ILambdaContext)
        : APIGatewayHttpApiV2ProxyResponse Task =
        async {
            use scope = serviceProvider.CreateScope()

            let poolIdResult =
                request.PathParameters
                |> Option.ofObj
                |> Option.defaultValue Map.empty
                |> tryGetStringParamOrError poolIdParameter

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

            let maybeSearchText =
                request.QueryStringParameters
                |> Option.ofObj
                |> Option.defaultValue Map.empty
                |> tryGetStringParamOrNone searchTextParameter

            match poolIdResult, gamblerIdResult, maybeNext, maybeSearchText with
            | Ok poolId, Ok gamblerId, next, searchText ->
                let! response =
                    getPendingBets
                        poolId
                        gamblerId
                        searchText
                        next
                        (scope.ServiceProvider.GetService<GetPendingPoolGamblerBetsQuery>())

                return! response.ToAmazonProxyResponse()
            | _ ->
                let errors =
                    [ toErrorOption poolIdResult; toErrorOption gamblerIdResult ] |> List.choose id

                return errors |> BadRequestResponseFactory.create
        }
        |> Async.StartAsTask

    // URL: /pools/{poolId}/gamblers/{gamblerId}/bets/finished
    member this.GetFinishedBets
        (request: APIGatewayHttpApiV2ProxyRequest, _: ILambdaContext)
        : APIGatewayHttpApiV2ProxyResponse Task =
        async {
            use scope = serviceProvider.CreateScope()

            let poolIdResult =
                request.PathParameters
                |> Option.ofObj
                |> Option.defaultValue Map.empty
                |> tryGetStringParamOrError poolIdParameter

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

            let maybeSearchText =
                request.QueryStringParameters
                |> Option.ofObj
                |> Option.defaultValue Map.empty
                |> tryGetStringParamOrNone searchTextParameter

            match poolIdResult, gamblerIdResult, maybeNext, maybeSearchText with
            | Ok poolId, Ok gamblerId, next, searchText ->
                let! response =
                    getFinishedBets
                        poolId
                        gamblerId
                        searchText
                        next
                        (scope.ServiceProvider.GetService<GetFinishedPoolGamblerBetsQuery>())

                return! response.ToAmazonProxyResponse()
            | _ ->
                let errors =
                    [ toErrorOption poolIdResult; toErrorOption gamblerIdResult ] |> List.choose id

                return errors |> BadRequestResponseFactory.create
        }
        |> Async.StartAsTask
