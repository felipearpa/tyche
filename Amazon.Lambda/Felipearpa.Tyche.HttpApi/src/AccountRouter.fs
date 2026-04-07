namespace Felipearpa.Tyche.HttpApi

open System
open Felipearpa.Tyche.Account.Application
open Felipearpa.Tyche.Function.AccountFunction
open Felipearpa.Tyche.Function.Request
open Microsoft.AspNetCore.Builder

[<AutoOpen>]
module AccountRouter =

    type WebApplication with

        member this.ConfigureAccountRoutes() =
            this
                .MapPost(
                    "/accounts",
                    Func<_, _, _>(fun (linkAccountRequest: LinkAccountRequest) (linkAccount: LinkAccount) ->
                        async { return! linkAccountAsync linkAccountRequest linkAccount }
                        |> Async.StartAsTask)
                )
                .RequireAuthorization()
            |> ignore

            this
