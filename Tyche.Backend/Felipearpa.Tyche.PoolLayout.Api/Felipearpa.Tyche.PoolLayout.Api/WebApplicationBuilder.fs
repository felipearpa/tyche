namespace Felipearpa.Tyche.PoolLayout.Api

open Amazon.DynamoDBv2
open Felipearpa.Core
open Felipearpa.Core.Json
open Felipearpa.Core.Jwt
open Felipearpa.Data.DynamoDb
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
module WebApplicationBuilder =

    let registerDependencies (app: WebApplicationBuilder) =
        app.Services
            .AddSingleton<IHasher, BCryptHasher>()
            .AddSingleton<ISerializer, JsonSerializer>()
            .AddSingleton<IKeySerializer, DynamoDbKeySerializer>()
            .AddSingleton<IJwtSetting, LocalJwtSetting>()
            .AddSingleton<IJwtGenerator, LocalJWTGenerator>()
            .AddDefaultAWSOptions(app.Configuration.GetAWSOptions())
            .AddAWSService<IAmazonDynamoDB>()
            .AddScoped<IPoolLayoutRepository, PoolLayoutDynamoDbRepository>()
            .AddScoped<GetOpenedPoolLayoutsQuery>()
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