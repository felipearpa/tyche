namespace Felipearpa.Tyche.HttpApi

#nowarn "3536"

open Amazon.DynamoDBv2
open Felipearpa.Core
open Felipearpa.Core.Json
open Felipearpa.Core.Jwt
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.Account.Application
open Felipearpa.Tyche.Account.Domain
open Felipearpa.Tyche.Account.Infrastructure
open Felipearpa.Tyche.Pool.Application
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Infrastructure
open Felipearpa.Tyche.PoolLayout.Application
open Felipearpa.Tyche.PoolLayout.Domain
open Felipearpa.Tyche.PoolLayout.Infrastructure
open Microsoft.AspNetCore.Builder
open Microsoft.AspNetCore.Authentication.JwtBearer
open Microsoft.Extensions.DependencyInjection
open Microsoft.IdentityModel.Tokens
open Microsoft.Extensions.Configuration
open Felipearpa.Crypto

[<AutoOpen>]
module DependencyInjectionConfigurator =

    let registerDependencies (app: WebApplicationBuilder) =
        app.Services
            .AddSingleton<IJwtSetting, LocalJwtSetting>()
            .AddSingleton<IHasher, BCryptHasher>()
            .AddSingleton<ISerializer, JsonSerializer>()
            .AddSingleton<IKeySerializer, DynamoDbKeySerializer>()
            .AddDefaultAWSOptions(app.Configuration.GetAWSOptions())
            .AddAWSService<IAmazonDynamoDB>()
            .AddScoped<IAccountRepository, AccountDynamoDbRepository>()
            .AddScoped<IGetAccountById, GetAccountById>()
            .AddScoped<LinkAccount>()
            .AddScoped<IPoolGamblerScoreRepository, PoolGamblerScoreDynamoDbRepository>()
            .AddScoped<IPoolGamblerBetRepository, PoolGamblerBetDynamoDbRepository>()
            .AddScoped<IPoolRepository, PoolDynamoDbRepository>()
            .AddScoped<GetPoolGamblerScoresByGambler>()
            .AddScoped<GetPoolGamblerScoresByPool>()
            .AddScoped<GetPendingPoolGamblerBets>()
            .AddScoped<GetFinishedPoolGamblerBets>()
            .AddScoped<GetPoolGamblerScoreById>()
            .AddScoped<GetPoolById>()
            .AddScoped<Bet>()
            .AddScoped<CreatePool>()
            .AddScoped<JoinPool>()
            .AddScoped<IPoolLayoutRepository, PoolLayoutDynamoDbRepository>()
            .AddScoped<GetOpenPoolLayouts>()
        |> ignore

    let registerJwt (app: WebApplicationBuilder) =
        app.Services.Configure<IJwtSetting>(app.Configuration.GetSection("Jwt"))
        |> ignore

        let provider = app.Services.BuildServiceProvider()

        let jwtSettings = provider.GetRequiredService<IJwtSetting>()

        app.Services
            .AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
            .AddJwtBearer(fun options ->
                options.Authority <- jwtSettings.Authority

                options.TokenValidationParameters <-
                    TokenValidationParameters(
                        ValidateIssuer = true,
                        ValidIssuer = jwtSettings.Issuer,
                        ValidateAudience = true,
                        ValidAudience = jwtSettings.Audience,
                        ValidateLifetime = true
                    ))
        |> ignore

        app.Services.AddAuthorization() |> ignore

    type WebApplicationBuilder with

        member this.RegisterServices() =
            registerDependencies this
            registerJwt this
            this
