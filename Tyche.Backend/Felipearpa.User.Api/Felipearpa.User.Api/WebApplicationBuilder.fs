namespace Felipearpa.User.Api

open System
open System.Text
open Amazon.DynamoDBv2
open Felipearpa.Core.Jwt
open Felipearpa.User.Application.UserCreation
open Felipearpa.User.Application.Login
open Felipearpa.User.Domain
open Felipearpa.User.Infrastructure
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
            .AddScoped<IUserRepository, UserDynamoDbRepository>()
            .AddScoped<CreateUserCommandHandler>()
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
                options.RequireHttpsMetadata <- false
                options.SaveToken <- true

                options.TokenValidationParameters <-
                    TokenValidationParameters(
                        ValidateIssuerSigningKey = true,
                        IssuerSigningKey = SymmetricSecurityKey(Encoding.ASCII.GetBytes(jwtSettings.Key)),
                        ValidateIssuer = false,
                        ValidateAudience = false,
                        ClockSkew = TimeSpan.Zero
                    ))
        |> ignore

        builder.Services.AddAuthorization() |> ignore

    type WebApplicationBuilder with

        member this.RegisterServices() =
            registerDependencies this
            registerJwt this
            this
