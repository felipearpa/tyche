namespace Felipearpa.Tyche.Pool.Api

open Microsoft.AspNetCore.Builder
open Microsoft.Extensions.Hosting
open Felipearpa.Tyche.Pool.Api

type Program() =
    class
    end

module Program =

    [<EntryPoint>]
    let main args =
        let builder = WebApplication.CreateBuilder(args)

        let app = builder.RegisterServices().Build()
        app.ConfigureMiddleware().ConfigureRoutes().Run()

        0 // Exit code
