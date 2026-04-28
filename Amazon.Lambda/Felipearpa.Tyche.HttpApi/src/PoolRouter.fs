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
                    Func<_, _, _>(fun (poolId: String) (getPoolById: GetPoolById) ->
                        async { return! getPoolByIdAsync poolId getPoolById } |> Async.StartAsTask)
                )
                .RequireAuthorization()
            |> ignore

            this
                .MapGet(
                    "/pools/{poolId}/gamblers",
                    Func<_, _, _, _>
                        (fun (poolId: string) (next: string) (getPoolGamblerScoresByPool: GetPoolGamblerScoresByPool) ->
                            async {
                                return!
                                    getGamblersByPoolIdAsync poolId (next |> Option.ofObj) getPoolGamblerScoresByPool
                            }
                            |> Async.StartAsTask)

                )
                .RequireAuthorization()
            |> ignore

            this
                .MapGet(
                    "/pools/{poolId}/gamblers/{gamblerId}",
                    Func<_, _, _, _>
                        (fun (poolId: string) (gamblerId: string) (getPoolGamblerScoreById: GetPoolGamblerScoreById) ->
                            async { return! getPoolGamblerScoreByIdAsync poolId gamblerId getPoolGamblerScoreById }
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
                            (getPendingPoolGamblerBets: GetPendingPoolGamblerBets) ->
                            async {
                                return!
                                    getPendingBetsAsync
                                        poolId
                                        gamblerId
                                        (searchText |> Option.ofObj)
                                        (next |> Option.ofObj)
                                        getPendingPoolGamblerBets
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
                            (getFinishedPoolGamblerBets: GetFinishedPoolGamblerBets) ->
                            async {
                                return!
                                    getFinishedBetsAsync
                                        poolId
                                        gamblerId
                                        (searchText |> Option.ofObj)
                                        (next |> Option.ofObj)
                                        getFinishedPoolGamblerBets
                            }
                            |> Async.StartAsTask)
                )
                .RequireAuthorization()
            |> ignore

            this
                .MapGet(
                    "/pools/{poolId}/gamblers/{gamblerId}/bets/live",
                    Func<_, _, _, _, _, _>
                        (fun
                            (poolId: string)
                            (gamblerId: string)
                            (searchText: string)
                            (next: string)
                            (getLivePoolGamblerBets: GetLivePoolGamblerBets) ->
                            async {
                                return!
                                    getLiveBetsAsync
                                        poolId
                                        gamblerId
                                        (searchText |> Option.ofObj)
                                        (next |> Option.ofObj)
                                        getLivePoolGamblerBets
                            }
                            |> Async.StartAsTask)
                )
                .RequireAuthorization()
            |> ignore

            this
                .MapPost(
                    "/pools",
                    Func<_, _, _>(fun (createPoolRequest: CreatePoolRequest) (createPool: CreatePool) ->
                        async { return! createPoolAsync createPoolRequest createPool }
                        |> Async.StartAsTask)
                )
                .RequireAuthorization()
            |> ignore

            this
                .MapPost(
                    "/pools/{poolId}/gamblers",
                    Func<_, _, _, _>(fun (poolId: string) (joinPoolRequest: JoinPoolRequest) (joinPool: JoinPool) ->
                        async { return! joinPoolAsync poolId joinPoolRequest joinPool }
                        |> Async.StartAsTask)
                )
                .RequireAuthorization()
            |> ignore

            this
