namespace Felipearpa.Tyche.HttpApi

open System
open Felipearpa.Tyche.Function.PoolFunction
open Felipearpa.Tyche.Function.Request
open Felipearpa.Tyche.Pool.Application
open Microsoft.AspNetCore.Builder

[<AutoOpen>]
module PoolRouter =

    type WebApplication with

        member this.ConfigurePoolRoutes() =
            this
                .MapGet(
                    "/pools/{poolId}",
                    Func<_, _, _>(fun (poolId: String) (getPoolByIdQuery: GetPoolByIdQuery) ->
                        async { return! getPoolById poolId getPoolByIdQuery } |> Async.StartAsTask)
                )
                .RequireAuthorization()
            |> ignore

            this
                .MapGet(
                    "/pools/{poolId}/gamblers",
                    Func<_, _, _, _>
                        (fun
                            (poolId: string)
                            (next: string)
                            (getPoolGamblerScoresByPoolQuery: GetPoolGamblerScoresByPoolQuery) ->
                            async {
                                return!
                                    getGamblersByPoolId poolId (next |> Option.ofObj) getPoolGamblerScoresByPoolQuery
                            }
                            |> Async.StartAsTask)

                )
                .RequireAuthorization()
            |> ignore

            this
                .MapGet(
                    "/pools/{poolId}/gamblers/{gamblerId}",
                    Func<_, _, _, _>
                        (fun
                            (poolId: string)
                            (gamblerId: string)
                            (getPoolGamblerScoreByIdQuery: GetPoolGamblerScoreByIdQuery) ->
                            async { return! getPoolGamblerScoreById poolId gamblerId getPoolGamblerScoreByIdQuery }
                            |> Async.StartAsTask)
                )
                .RequireAuthorization()
            |> ignore

            this
                .MapGet(
                    "/pools/{poolId}/gamblers/{gamblerId}/bets/pending",
                    Func<_, _, _, _, _, _>
                        (fun
                            (poolId: string)
                            (gamblerId: string)
                            (searchText: string)
                            (next: string)
                            (getPendingPoolGamblerBetsQuery: GetPendingPoolGamblerBetsQuery) ->
                            async {
                                return!
                                    getPendingBets
                                        poolId
                                        gamblerId
                                        (searchText |> Option.ofObj)
                                        (next |> Option.ofObj)
                                        getPendingPoolGamblerBetsQuery
                            }
                            |> Async.StartAsTask)
                )
                .RequireAuthorization()
            |> ignore

            this
                .MapGet(
                    "/pools/{poolId}/gamblers/{gamblerId}/bets/finished",
                    Func<_, _, _, _, _, _>
                        (fun
                            (poolId: string)
                            (gamblerId: string)
                            (searchText: string)
                            (next: string)
                            (getFinishedPoolGamblerBetsQuery: GetFinishedPoolGamblerBetsQuery) ->
                            async {
                                return!
                                    getFinishedBets
                                        poolId
                                        gamblerId
                                        (searchText |> Option.ofObj)
                                        (next |> Option.ofObj)
                                        getFinishedPoolGamblerBetsQuery
                            }
                            |> Async.StartAsTask)
                )
                .RequireAuthorization()
            |> ignore

            this
                .MapPost(
                    "/pools",
                    Func<_, _, _>(fun (createPoolRequest: CreatePoolRequest) (createPoolCommand: CreatePoolCommand) ->
                        async { return! createPool createPoolRequest createPoolCommand }
                        |> Async.StartAsTask)
                )
                .RequireAuthorization()
            |> ignore

            this
