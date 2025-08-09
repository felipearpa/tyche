namespace Felipearpa.Tyche.HttpApi

open System
open Felipearpa.Tyche.Function.BetFunction
open Felipearpa.Tyche.Function.Request
open Felipearpa.Tyche.Pool.Application
open Microsoft.AspNetCore.Builder

[<AutoOpen>]
module BetRouter =

    type WebApplication with

        member this.ConfigureBetRoutes() =
            this
                .MapPatch(
                    "/bets",
                    Func<_, _, _>(fun (betRequest: BetRequest) (betCommand: BetCommand) ->
                        async { return! bet betRequest betCommand } |> Async.StartAsTask)

                )
                .RequireAuthorization()
            |> ignore

            this
