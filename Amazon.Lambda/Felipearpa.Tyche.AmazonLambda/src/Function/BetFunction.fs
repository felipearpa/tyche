namespace Felipearpa.Tyche.AmazonLambda.Function

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
open Felipearpa.Tyche.AmazonLambda
open Felipearpa.Tyche.Function.BetFunction
open Felipearpa.Tyche.Function.Request
open Felipearpa.Tyche.Pool.Application
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Infrastructure
open Felipearpa.Type
open Microsoft.Extensions.DependencyInjection
open Microsoft.Extensions.Logging

type BetFunction(configureServices: IServiceCollection -> unit) =

    let buildServiceProvider () =
        let services = ServiceCollection()

        services
            .AddAWSService<IAmazonDynamoDB>()
            .AddLogging(fun builder -> builder.AddLambdaLogger() |> ignore)
            .AddSingleton<ISerializer, JsonSerializer>()
            .AddSingleton<IKeySerializer, DynamoDbKeySerializer>()
            .AddScoped<IPoolGamblerScoreRepository, PoolGamblerScoreDynamoDbRepository>()
            .AddScoped<IPoolGamblerBetRepository, PoolGamblerBetDynamoDbRepository>()
            .AddScoped<IPoolRepository, PoolDynamoDbRepository>()
            .AddScoped<IAccountRepository, AccountDynamoDbRepository>()
            .AddScoped<IGetAccountById, GetAccountById>()
            .AddScoped<GetPoolGamblerScoresByGambler>()
            .AddScoped<GetPoolGamblerScoresByPool>()
            .AddScoped<GetPendingPoolGamblerBets>()
            .AddScoped<GetFinishedPoolGamblerBets>()
            .AddScoped<GetLivePoolGamblerBets>()
            .AddScoped<GetPoolGamblerScoreById>()
            .AddScoped<GetPoolById>()
            .AddScoped<Bet>()
            .AddScoped<CreatePool>()
            .AddScoped<JoinPool>()
        |> ignore

        configureServices services

        services.BuildServiceProvider()

    let serviceProvider = buildServiceProvider ()

    new() = BetFunction(fun _ -> ())

    // PATCH /bets
    member this.BetAsync
        (request: APIGatewayHttpApiV2ProxyRequest, _: ILambdaContext)
        : APIGatewayHttpApiV2ProxyResponse Task =
        async {
            use scope = serviceProvider.CreateScope()

            let betRequestResult = tryGetOrError<BetRequest> request.Body

            match betRequestResult with
            | Error error -> return [ error ] |> BadRequestResponseFactory.create
            | Ok betPoolRequest ->
                let! callerResult =
                    Authorization.resolveCallerGamblerIdAsync
                        request
                        (scope.ServiceProvider.GetService<IAccountRepository>())

                match callerResult with
                | Error failure -> return Authorization.toResponse failure
                | Ok callerGamblerId when callerGamblerId.Value <> betPoolRequest.GamblerId ->
                    return Authorization.toResponse NotAMember
                | Ok _ ->
                    let! response = betAsync betPoolRequest (scope.ServiceProvider.GetService<Bet>())
                    return! response.ToAmazonProxyResponse()
        }
        |> Async.StartAsTask
