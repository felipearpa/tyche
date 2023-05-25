namespace Felipearpa.Tyche.Pool.Api

open System
open Felipearpa.Core.Paging
open Felipearpa.Tyche.Pool.Api.ViewModel
open Felipearpa.Tyche.Pool.Application
open Microsoft.AspNetCore.Builder
open Felipearpa.Type
open Felipearpa.Tyche.Pool.Type
open Microsoft.AspNetCore.Http

[<AutoOpen>]
module WebApplication =

    type WebApplication with

        member this.ConfigureMiddleware() =
            this.UseAuthentication().UseAuthorization() |> ignore

            this

        member this.ConfigureRoutes() =
            this
                .MapGet(
                    "/gambler/{gamblerId}/pools",
                    Func<_, _, _, _, _>
                        (fun
                            (gamblerId: string)
                            (searchText: string)
                            (next: string)
                            (getPoolGamblerScoresByGamblerQuery: GetPoolGamblerScoresByGamblerQuery) ->
                            async {
                                let! page =
                                    getPoolGamblerScoresByGamblerQuery.ExecuteAsync(
                                        gamblerId |> Ulid.newOf,
                                        searchText |> Option.ofObj,
                                        next |> Option.ofObj
                                    )

                                return page |> CursorPage.map PoolGamblerScoreMapper.mapToViewModel
                            }
                            |> Async.StartAsTask

                        )
                )
                .RequireAuthorization()
            |> ignore

            this
                .MapGet(
                    "/pool/{poolId}/gamblers",
                    Func<_, _, _, _, _>
                        (fun
                            (poolId: string)
                            (searchText: string)
                            (next: string)
                            (getPoolGamblerScoresByPoolQuery: GetPoolGamblerScoresByPoolQuery) ->
                            async {
                                let! page =
                                    getPoolGamblerScoresByPoolQuery.ExecuteAsync(
                                        poolId |> Ulid.newOf,
                                        searchText |> Option.ofObj,
                                        next |> Option.ofObj
                                    )

                                return page |> CursorPage.map PoolGamblerScoreMapper.mapToViewModel

                            }
                            |> Async.StartAsTask

                        )

                )
                .RequireAuthorization()
            |> ignore

            this
                .MapGet(
                    "/pool/{poolId}",
                    Func<_, _, _>(fun (poolId: string) (getPoolQuery: GetPoolQuery) ->
                        async {
                            let! maybePool = getPoolQuery.ExecuteAsync(poolId |> Ulid.newOf)

                            return
                                match maybePool with
                                | Some pool -> Results.Ok(pool |> PoolMapper.mapToViewModel)
                                | None -> Results.NotFound()
                        }
                        |> Async.StartAsTask)
                )
                .RequireAuthorization()
            |> ignore

            this.MapGet(
                "/pool/{poolId}/gambler/{gamblerId}/bets",
                Func<_, _, _, _, _, _>
                    (fun
                        (poolId: string)
                        (gamblerId: string)
                        (searchText: string)
                        (next: string)
                        (getPoolGamblerBetsQuery: GetPoolGamblerBetsQuery) ->
                        async {
                            let! page =
                                getPoolGamblerBetsQuery.ExecuteAsync(
                                    poolId |> Ulid.newOf,
                                    gamblerId |> Ulid.newOf,
                                    searchText |> Option.ofObj,
                                    next |> Option.ofObj
                                )

                            return page |> CursorPage.map PoolGamblerBetMapper.mapToViewModel
                        }
                        |> Async.StartAsTask)
            )

            |> ignore

            this
                .MapPut(
                    "/bet",
                    Func<_, _, _>(fun (betRequest: BetRequest) (betCommand: BetCommand) ->
                        async {
                            let betScore =
                                { TeamScore.HomeTeamValue = betRequest.HomeTeamBet |> BetScore.newOf
                                  AwayTeamValue = betRequest.AwayTeamBet |> BetScore.newOf }

                            let! result =
                                betCommand.ExecuteAsync(
                                    betRequest.PoolId |> Ulid.newOf,
                                    betRequest.GamblerId |> Ulid.newOf,
                                    betRequest.MatchId |> Ulid.newOf,
                                    betScore
                                )

                            return
                                match result with
                                | Ok poolGamblerBet ->
                                    Results.Ok(poolGamblerBet |> PoolGamblerBetMapper.mapToViewModel)
                                | Error _ -> Results.Forbid()
                        }
                        |> Async.StartAsTask)
                )
                .RequireAuthorization()
            |> ignore

            this
