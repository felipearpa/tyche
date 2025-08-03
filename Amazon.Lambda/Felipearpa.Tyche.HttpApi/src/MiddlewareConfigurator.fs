namespace Felipearpa.Tyche.HttpApi

open Microsoft.AspNetCore.Builder

[<AutoOpen>]
module MiddlewareConfigurator =

    type WebApplication with

        member this.ConfigureMiddleware() =
            this.UseAuthentication().UseAuthorization() |> ignore
            this
