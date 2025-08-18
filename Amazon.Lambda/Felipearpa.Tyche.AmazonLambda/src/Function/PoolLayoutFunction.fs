namespace Felipearpa.Tyche.AmazonLambda.Function

#nowarn "3536"

open System.Threading.Tasks
open Amazon.DynamoDBv2
open Amazon.Lambda.APIGatewayEvents
open Amazon.Lambda.Core
open Felipearpa.Core
open Felipearpa.Core.Json
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.AmazonLambda
open Felipearpa.Tyche.Function.PoolLayoutFunction
open Felipearpa.Tyche.PoolLayout.Application
open Felipearpa.Tyche.PoolLayout.Domain
open Felipearpa.Tyche.PoolLayout.Infrastructure
open Microsoft.Extensions.DependencyInjection
open Microsoft.Extensions.Logging

type PoolLayoutFunction(configureServices: IServiceCollection -> unit) =

    [<Literal>]
    let nextParameter = "next"

    let buildServiceProvider () =
        let services = ServiceCollection()

        services
            .AddAWSService<IAmazonDynamoDB>()
            .AddLogging(fun builder -> builder.AddLambdaLogger() |> ignore)
            .AddSingleton<ISerializer, JsonSerializer>()
            .AddSingleton<IKeySerializer, DynamoDbKeySerializer>()
            .AddScoped<IPoolLayoutRepository, PoolLayoutDynamoDbRepository>()
            .AddScoped<GetOpenPoolLayoutsQuery>()
        |> ignore

        configureServices services

        services.BuildServiceProvider()

    let serviceProvider = buildServiceProvider ()

    new() = PoolLayoutFunction(fun _ -> ())

    // GET /pool-layouts/open
    member this.GetOpenPoolLayoutsAsync
        (request: APIGatewayHttpApiV2ProxyRequest, _: ILambdaContext)
        : APIGatewayHttpApiV2ProxyResponse Task =
        async {
            use scope = serviceProvider.CreateScope()

            let maybeNext =
                request.QueryStringParameters
                |> Option.ofObj
                |> Option.defaultValue Map.empty
                |> tryGetStringParamOrNone nextParameter

            let! response =
                getOpenPoolLayoutsAsync
                    (maybeNext |> Option.bind noneIfEmpty)
                    (scope.ServiceProvider.GetService<GetOpenPoolLayoutsQuery>())

            return! response.ToAmazonProxyResponse()
        }
        |> Async.StartAsTask
