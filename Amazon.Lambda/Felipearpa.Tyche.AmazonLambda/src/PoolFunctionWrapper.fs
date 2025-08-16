namespace Felipearpa.Tyche.AmazonLambda

#nowarn "3536"

open System.Threading.Tasks
open Amazon.DynamoDBv2
open Amazon.Lambda.APIGatewayEvents
open Amazon.Lambda.Core
open Felipearpa.Core
open Felipearpa.Core.Json
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.Account.Application
open Felipearpa.Tyche.Account.Domain
open Felipearpa.Tyche.Account.Infrastructure
open Felipearpa.Tyche.AmazonLambda.StringTransformer
open Felipearpa.Tyche.Function.PoolFunction
open Felipearpa.Tyche.Function.Request
open Felipearpa.Tyche.Pool.Application
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
            .AddScoped<IAccountRepository, AccountDynamoDbRepository>()
            .AddScoped<IGetAccountById, GetAccountByIdQuery>()
        |> ignore

        configureServices services

        services.BuildServiceProvider()

    let serviceProvider = buildServiceProvider ()

    new() = PoolFunctionWrapper(fun _ -> ())

    // GET /pools/{poolId}
    member this.GetPoolByIdAsync
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
                let! response = getPoolByIdAsync poolId (scope.ServiceProvider.GetService<GetPoolByIdQuery>())
                return! response.ToAmazonProxyResponse()
        }
        |> Async.StartAsTask

    // GET /pools/{poolId}/gamblers?next={next}
    member this.GetGamblersByPoolIdAsync
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
                    getGamblersByPoolIdAsync
                        poolId
                        (next |> Option.bind noneIfEmpty)
                        (scope.ServiceProvider.GetService<GetPoolGamblerScoresByPoolQuery>())

                return! response.ToAmazonProxyResponse()
            | _ ->
                let errors = [ toErrorOption poolIdResult ] |> List.choose id

                return errors |> BadRequestResponseFactory.create
        }
        |> Async.StartAsTask

    // GET /pools/{poolId}/gamblers/{gamblerId}
    member this.GetPoolGamblerScoreByIdAsync
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
                    getPoolGamblerScoreByIdAsync
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

    // GET /pools/{poolId}/gamblers/{gamblerId}/bets/pending
    member this.GetPendingBetsAsync
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
                    getPendingBetsAsync
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

    // GET /pools/{poolId}/gamblers/{gamblerId}/bets/finished
    member this.GetFinishedBetsAsync
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
                    getFinishedBetsAsync
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

    // POST /pools
    member this.CreatePoolAsync
        (request: APIGatewayHttpApiV2ProxyRequest, _: ILambdaContext)
        : APIGatewayHttpApiV2ProxyResponse Task =
        async {
            use scope = serviceProvider.CreateScope()

            let createPoolRequestResult = tryGetOrError<CreatePoolRequest> request.Body

            match createPoolRequestResult with
            | Error error -> return [ error ] |> BadRequestResponseFactory.create
            | Ok createPoolRequest ->
                let! response =
                    createPoolAsync createPoolRequest (scope.ServiceProvider.GetService<CreatePoolCommand>())

                return! response.ToAmazonProxyResponse()
        }
        |> Async.StartAsTask

    // POST /pools/{poolId}/gamblers
    member this.JoinPoolAsync
        (request: APIGatewayHttpApiV2ProxyRequest, _: ILambdaContext)
        : APIGatewayHttpApiV2ProxyResponse Task =
        async {
            use scope = serviceProvider.CreateScope()

            let poolIdResult =
                request.PathParameters
                |> Option.ofObj
                |> Option.defaultValue Map.empty
                |> tryGetStringParamOrError poolIdParameter

            let joinPoolRequestResult = tryGetOrError<JoinPoolRequest> request.Body

            match poolIdResult, joinPoolRequestResult with
            | Ok poolId, Ok joinPoolRequest ->
                let! response =
                    joinPoolAsync poolId joinPoolRequest (scope.ServiceProvider.GetService<JoinPoolCommand>())

                return! response.ToAmazonProxyResponse()
            | _ ->
                let errors =
                    [ toErrorOption poolIdResult; toErrorOption joinPoolRequestResult ]
                    |> List.choose id

                return errors |> BadRequestResponseFactory.create
        }
        |> Async.StartAsTask
