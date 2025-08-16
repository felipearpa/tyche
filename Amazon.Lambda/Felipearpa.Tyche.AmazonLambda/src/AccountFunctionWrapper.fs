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
open Felipearpa.Tyche.Function.AccountFunction
open Felipearpa.Tyche.Function.Request
open Microsoft.Extensions.DependencyInjection

type AccountFunctionWrapper(configureServices: IServiceCollection -> unit) =

    let buildServiceProvider () =
        let services = ServiceCollection()

        services
            .AddAWSService<IAmazonDynamoDB>()
            .AddSingleton<ISerializer, JsonSerializer>()
            .AddSingleton<IKeySerializer, DynamoDbKeySerializer>()
            .AddScoped<IAccountRepository, AccountDynamoDbRepository>()
            .AddScoped<LinkAccountCommand>()
        |> ignore

        configureServices services

        services.BuildServiceProvider()

    let serviceProvider = buildServiceProvider ()

    new() = AccountFunctionWrapper(fun _ -> ())

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
                let! response =
                    linkAccountAsync linkAccountRequest (scope.ServiceProvider.GetService<LinkAccountCommand>())

                return! response.ToAmazonProxyResponse()
        }
        |> Async.StartAsTask
