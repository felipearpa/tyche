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
                            (gamblerId: string)
                            (next: string)
                            (getPoolGamblerScoresByGambler: GetPoolGamblerScoresByGambler) ->
                            async {
                                return!
                                    getPoolsByGamblerIdAsync
                                        gamblerId
                                        (next |> Option.ofObj)
                                        getPoolGamblerScoresByGambler
                            }
                            |> Async.StartAsTask)

                )
                .RequireAuthorization()
            |> ignore

            this
