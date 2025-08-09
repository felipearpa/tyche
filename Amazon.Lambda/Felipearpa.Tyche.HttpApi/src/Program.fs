open Felipearpa.Tyche.HttpApi
open Microsoft.AspNetCore.Builder
open Microsoft.Extensions.Hosting

[<EntryPoint>]
let main args =
    let builder = WebApplication.CreateBuilder(args)
    let app = builder.RegisterServices().Build()

    app
        .ConfigureMiddleware()
        .ConfigurePoolRoutes()
        .ConfigureGamblerRoutes()
        .ConfigureBetRoutes()
        .ConfigurePoolLayoutRoutes()
        .Run()

    0 // Exit code
