namespace Pipel.Tyche.Pool.Api

open Microsoft.AspNetCore.Builder
open Microsoft.Extensions.Hosting
open Pipel.Tyche.Pool.Api
open WebApplicationInit
open WebApplicationBuilderInit

type Program() =
    class
    end

module Program =

    [<EntryPoint>]
    let main args =
        let builder = WebApplication.CreateBuilder(args)
        builder.Init()
        let app = builder.Build()
        app.Init()
        app.Run()
        0 // Exit code
