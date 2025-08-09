namespace Felipearpa.Tyche.HttpApi

open System
open Felipearpa.Tyche.Function.PoolLayoutFunction
open Felipearpa.Tyche.PoolLayout.Application
open Microsoft.AspNetCore.Builder

[<AutoOpen>]
module PoolLayoutRouter =

    type WebApplication with

        member this.ConfigurePoolLayoutRoutes() =
            this
                .MapGet(
                    "/pool-layouts/open",
                    Func<_, _, _>(fun (next: string) (getOpenedPoolLayoutsQuery: GetOpenPoolLayoutsQuery) ->
                        async { return! getOpenPoolLayoutsAsync (next |> Option.ofObj) getOpenedPoolLayoutsQuery }
                        |> Async.StartAsTask

                    )
                )
                .RequireAuthorization()
            |> ignore

            this
