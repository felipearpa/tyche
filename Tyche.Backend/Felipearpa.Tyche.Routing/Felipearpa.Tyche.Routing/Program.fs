namespace Felipearpa.Tyche.Routing

#nowarn "20"

open Microsoft.AspNetCore.Builder
open Microsoft.Extensions.DependencyInjection
open Microsoft.Extensions.Hosting
open Microsoft.Extensions.Configuration
open Ocelot.DependencyInjection
open Ocelot.Middleware

module Program =
    let exitCode = 0

    [<EntryPoint>]
    let main args =

        let builder = WebApplication.CreateBuilder(args)

        let isOcelotOptional = false
        let isOcelotReloadOnChange = true
        builder.Configuration.AddJsonFile("ocelot.json", isOcelotOptional, isOcelotReloadOnChange)

        builder.Services.AddControllers()
        builder.Services.AddOcelot(builder.Configuration)

        let app = builder.Build()

        app.UseHttpsRedirection()

        app.UseAuthorization()
        app.UseAuthentication()

        app.MapControllers()

        app.UseOcelot().Wait()

        app.Run()

        exitCode
