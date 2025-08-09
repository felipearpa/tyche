namespace Felipearpa.Tyche.AmazonLambda

#nowarn "3536"

open System.Threading.Tasks
open Amazon.DynamoDBv2
open Amazon.Lambda.APIGatewayEvents
open Amazon.Lambda.Core
open Felipearpa.Core
open Felipearpa.Core.Json
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.Function.PoolLayoutFunction
open Felipearpa.Tyche.PoolLayout.Application
open Felipearpa.Tyche.PoolLayout.Domain
open Felipearpa.Tyche.PoolLayout.Infrastructure
open Microsoft.Extensions.DependencyInjection

type PoolLayoutFunctionWrapper(configureServices: IServiceCollection -> unit) =

    [<Literal>]
    let nextParameter = "next"

    let buildServiceProvider () =
        let services = ServiceCollection()

        services
            .AddAWSService<IAmazonDynamoDB>()
            .AddSingleton<ISerializer, JsonSerializer>()
            .AddSingleton<IKeySerializer, DynamoDbKeySerializer>()
            .AddScoped<IPoolLayoutRepository, PoolLayoutDynamoDbRepository>()
            .AddScoped<GetOpenPoolLayoutsQuery>()
        |> ignore

        configureServices services

        services.BuildServiceProvider()

    let serviceProvider = buildServiceProvider ()

    new() = PoolLayoutFunctionWrapper(fun _ -> ())

    // GET /pool-layouts/open
    member this.GetOpenPoolLayouts
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
                getOpenPoolLayouts
                    (maybeNext |> Option.bind noneIfEmpty)
                    (scope.ServiceProvider.GetService<GetOpenPoolLayoutsQuery>())

            return! response.ToAmazonProxyResponse()
        }
        |> Async.StartAsTask
