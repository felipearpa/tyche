namespace Felipearpa.Account.Api

open System
open System.Text
open Amazon.DynamoDBv2
open Felipearpa.Core.Jwt
open Felipearpa.Account.Application.SignIn
open Felipearpa.Account.Domain
open Felipearpa.Account.Infrastructure
open Microsoft.AspNetCore.Authentication.JwtBearer
open Microsoft.AspNetCore.Builder
open Microsoft.Extensions.DependencyInjection
open Microsoft.Extensions.Configuration
open Felipearpa.Crypto
open Microsoft.IdentityModel.Tokens

[<AutoOpen>]
module WebApplicationBuilder =

    let registerDependencies (app: WebApplicationBuilder) =
        app.Services
            .AddSingleton<IHasher, BCryptHasher>()
            .AddSingleton<IJwtSetting, LocalJwtSetting>()
            .AddSingleton<IJwtGenerator, LocalJWTGenerator>()
            .AddDefaultAWSOptions(app.Configuration.GetAWSOptions())
            .AddAWSService<IAmazonDynamoDB>()
            .AddScoped<IAccountRepository, AccountDynamoDbRepository>()
            .AddScoped<LoginCommandHandler>()
        |> ignore

    let registerJwt (builder: WebApplicationBuilder) =
        builder.Services.Configure<IJwtSetting>(builder.Configuration.GetSection("Jwt"))
        |> ignore

        let provider = builder.Services.BuildServiceProvider()

        let jwtSettings = provider.GetRequiredService<IJwtSetting>()

        builder.Services
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

        builder.Services.AddAuthorization() |> ignore

    type WebApplicationBuilder with

        member this.RegisterServices() =
            registerDependencies this
            registerJwt this
            this
