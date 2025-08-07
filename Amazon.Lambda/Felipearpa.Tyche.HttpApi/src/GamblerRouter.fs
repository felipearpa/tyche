namespace Felipearpa.Tyche.HttpApi

open System
open Felipearpa.Tyche.Function.GamblerFunction
open Felipearpa.Tyche.Pool.Application
open Microsoft.AspNetCore.Builder

[<AutoOpen>]
module GamblerRouter =

    type WebApplication with

        member this.ConfigureGamblerRoutes() =
            this
                .MapGet(
                    "/gamblers/{gamblerId}/pools",
                    Func<_, _, _, _>
                        (fun
                            (poolId: string)
                            (next: string)
                            (getPoolGamblerScoresByGamblerQuery: GetPoolGamblerScoresByGamblerQuery) ->
                            async {
                                return!
                                    getPoolsByGamblerId
                                        poolId
                                        (next |> Option.ofObj)
                                        getPoolGamblerScoresByGamblerQuery
                            }
                            |> Async.StartAsTask)

                )
                .RequireAuthorization()
            |> ignore

            this
