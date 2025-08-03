namespace Felipearpa.Tyche.Function

open System.Runtime.CompilerServices
open Amazon.DynamoDBv2
open Felipearpa.Core
open Felipearpa.Core.Json
open Felipearpa.Core.Jwt
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.Account.Application
open Felipearpa.Tyche.Account.Application.SignIn
open Felipearpa.Tyche.Account.Domain
open Felipearpa.Tyche.Account.Infrastructure
open Felipearpa.Tyche.Pool.Application
open Felipearpa.Tyche.Pool.Data
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Infrastructure
open Felipearpa.Tyche.PoolLayout.Application
open Felipearpa.Tyche.PoolLayout.Domain
open Felipearpa.Tyche.PoolLayout.Infrastructure
open Microsoft.Extensions.DependencyInjection
open Felipearpa.Crypto

type public Extensions() =

    [<Extension>]
    static member public RegisterServices(this: IServiceCollection) =
        this
            .AddSingleton<IHasher, BCryptHasher>()
            .AddSingleton<ISerializer, JsonSerializer>()
            .AddSingleton<IKeySerializer, DynamoDbKeySerializer>()
            .AddSingleton<IJwtSetting, LocalJwtSetting>()
            .AddSingleton<IJwtGenerator, LocalJWTGenerator>()
            .AddAWSService<IAmazonDynamoDB>()
            .AddScoped<IAccountRepository, AccountDynamoDbRepository>()
            .AddScoped<IGetAccountById, GetAccountByIdQuery>()
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
            .AddScoped<IPoolLayoutRepository, PoolLayoutDynamoDbRepository>()
            .AddScoped<GetOpenPoolLayoutsQuery>()
            .AddScoped<IAccountRepository, AccountDynamoDbRepository>()
            .AddScoped<LoginCommandHandler>()
        |> ignore

        this
