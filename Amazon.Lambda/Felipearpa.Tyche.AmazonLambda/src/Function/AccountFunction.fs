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
open Felipearpa.Tyche.Function.AccountFunction
open Felipearpa.Tyche.Function.Request
open Felipearpa.Tyche.Pool.Application
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Infrastructure
open Microsoft.Extensions.DependencyInjection
open Microsoft.Extensions.Logging

type AccountFunction(configureServices: IServiceCollection -> unit) =

    let buildServiceProvider () =
        let services = ServiceCollection()

        services
            .AddAWSService<IAmazonDynamoDB>()
            .AddLogging(fun builder -> builder.AddLambdaLogger() |> ignore)
            .AddSingleton<ISerializer, JsonSerializer>()
            .AddSingleton<IKeySerializer, DynamoDbKeySerializer>()
            .AddScoped<IAccountRepository, AccountDynamoDbRepository>()
            .AddScoped<IPoolRepository, PoolDynamoDbRepository>()
            .AddScoped<LinkAccount>()
            .AddScoped<UpdateUsername>()
        |> ignore

        configureServices services

        services.BuildServiceProvider()

    let serviceProvider = buildServiceProvider ()

    new() = AccountFunction(fun _ -> ())

    // POST /accounts
    member this.LinkAccountAsync
        (request: APIGatewayHttpApiV2ProxyRequest, _: ILambdaContext)
        : APIGatewayHttpApiV2ProxyResponse Task =
        async {
            use scope = serviceProvider.CreateScope()

            let linkAccountRequestResult = tryGetOrError<LinkAccountRequest> request.Body

            match linkAccountRequestResult with
            | Error error -> return [ error ] |> BadRequestResponseFactory.create
            | Ok linkAccountRequest ->
                let! response = linkAccountAsync linkAccountRequest (scope.ServiceProvider.GetService<LinkAccount>())

                return! response.ToAmazonProxyResponse()
        }
        |> Async.StartAsTask

    // PATCH /accounts
    member this.UpdateUsernameAsync
        (request: APIGatewayHttpApiV2ProxyRequest, _: ILambdaContext)
        : APIGatewayHttpApiV2ProxyResponse Task =
        async {
            use scope = serviceProvider.CreateScope()

            let updateUsernameRequestResult = tryGetOrError<UpdateUsernameRequest> request.Body

            match updateUsernameRequestResult with
            | Error error -> return [ error ] |> BadRequestResponseFactory.create
            | Ok updateUsernameRequest ->
                let! callerResult =
                    Authorization.resolveCallerGamblerIdAsync
                        request
                        (scope.ServiceProvider.GetService<IAccountRepository>())

                match callerResult with
                | Error failure -> return Authorization.toResponse failure
                | Ok callerAccountId when callerAccountId.Value <> updateUsernameRequest.AccountId ->
                    return Authorization.toResponse NotAMember
                | Ok _ ->
                    let! response =
                        updateUsernameAsync updateUsernameRequest (scope.ServiceProvider.GetService<UpdateUsername>())

                    return! response.ToAmazonProxyResponse()
        }
        |> Async.StartAsTask
