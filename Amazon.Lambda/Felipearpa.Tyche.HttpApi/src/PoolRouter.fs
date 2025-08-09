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
                        async { return! getPoolByIdAsync poolId getPoolByIdQuery } |> Async.StartAsTask)
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
                                    getGamblersByPoolIdAsync poolId (next |> Option.ofObj) getPoolGamblerScoresByPoolQuery
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
                            async { return! getPoolGamblerScoreByIdAsync poolId gamblerId getPoolGamblerScoreByIdQuery }
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
                                    getPendingBetsAsync
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
                                    getFinishedBetsAsync
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
                        async { return! createPoolAsync createPoolRequest createPoolCommand }
                        |> Async.StartAsTask)
                )
                .RequireAuthorization()
            |> ignore

            this
                .MapPost(
                    "/pools/join",
                    Func<_, _, _>(fun (joinPoolRequest: JoinPoolRequest) (joinPoolCommand: JoinPoolCommand) ->
                        async { return! joinPoolAsync joinPoolRequest joinPoolCommand } |> Async.StartAsTask)
                )
                .RequireAuthorization()
            |> ignore

            this
