namespace Felipearpa.Tyche.Pool.Api

open System
open System.Net
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
                    "/gamblers/{gamblerId}/pools",
                    Func<_, _, _, _>
                        (fun
                            (gamblerId: string)
                            (next: string)
                            (getPoolGamblerScoresByGamblerQuery: GetPoolGamblerScoresByGamblerQuery) ->
                            async {
                                let! page =
                                    getPoolGamblerScoresByGamblerQuery.ExecuteAsync(
                                        gamblerId |> Ulid.newOf,
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
                    "/pools/{poolId}/gamblers",
                    Func<_, _, _, _>
                        (fun
                            (poolId: string)
                            (next: string)
                            (getPoolGamblerScoresByPoolQuery: GetPoolGamblerScoresByPoolQuery) ->
                            async {
                                let! page =
                                    getPoolGamblerScoresByPoolQuery.ExecuteAsync(
                                        poolId |> Ulid.newOf,
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
                    "/pools/{poolId}/gamblers/{gamblerId}",
                    Func<_, _, _, _>
                        (fun (poolId: string) (gamblerId: string) (getPoolGamblerScoreQuery: GetPoolGamblerScoreQuery) ->
                            async {
                                let! result =
                                    getPoolGamblerScoreQuery.ExecuteAsync(
                                        poolId |> Ulid.newOf,
                                        gamblerId |> Ulid.newOf
                                    )

                                return
                                    match result with
                                    | Ok maybePoolGamblerScore ->
                                        match maybePoolGamblerScore with
                                        | None -> Results.NotFound()
                                        | Some poolGamblerScore ->
                                            Results.Ok(poolGamblerScore |> PoolGamblerScoreMapper.mapToViewModel)
                                    | Error _ -> Results.StatusCode(int HttpStatusCode.InternalServerError)
                            }
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
                                let! page =
                                    getPendingPoolGamblerBetsQuery.ExecuteAsync(
                                        poolId |> Ulid.newOf,
                                        gamblerId |> Ulid.newOf,
                                        searchText |> Option.ofObj,
                                        next |> Option.ofObj
                                    )

                                return page |> CursorPage.map PoolGamblerBetMapper.mapToViewModel
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
                                let! page =
                                    getFinishedPoolGamblerBetsQuery.ExecuteAsync(
                                        poolId |> Ulid.newOf,
                                        gamblerId |> Ulid.newOf,
                                        searchText |> Option.ofObj,
                                        next |> Option.ofObj
                                    )

                                return page |> CursorPage.map PoolGamblerBetMapper.mapToViewModel
                            }
                            |> Async.StartAsTask)
                )
                .RequireAuthorization()
            |> ignore

            this
                .MapPatch(
                    "/bets",
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
                .MapGet(
                    "/pools/{poolId}",
                    Func<_, _, _>(fun (poolId: String) (getPoolByIdQuery: GetPoolByIdQuery) ->
                        async {
                            let! result = getPoolByIdQuery.ExecuteAsync(poolId |> Ulid.newOf)

                            return
                                match result with
                                | Ok maybePool ->
                                    match maybePool with
                                    | None -> Results.NotFound("Pool not found")
                                    | Some pool -> Results.Ok(pool |> PoolTransformer.toPoolViewModel)
                                | Error _ -> Results.InternalServerError()
                        }
                        |> Async.StartAsTask)
                )
                .RequireAuthorization()
            |> ignore

            this
                .MapPost(
                    "/pools",
                    Func<_, _, _>(fun (createPoolRequest: CreatePoolRequest) (createPoolCommand: CreatePoolCommand) ->
                        async {
                            let! result =
                                createPoolCommand.ExecuteAsync(
                                    createPoolRequest |> CreatePoolRequestTransformer.toCreatePoolInput
                                )

                            return
                                match result with
                                | Ok pool -> Results.Ok(pool |> CreatePoolOutputTransformer.toPoolViewModel)
                                | Error _ -> Results.NotFound("Gambler not found")
                        }
                        |> Async.StartAsTask)
                )
                .RequireAuthorization()
            |> ignore

            this
                .MapPost(
                    "/pools/join",
                    Func<_, _, _>(fun (joinPoolRequest: JoinPoolRequest) (joinPoolCommand: JoinPoolCommand) ->
                        async {
                            let! result =
                                joinPoolCommand.ExecuteAsync(
                                    joinPoolRequest |> JoinPoolRequestTransformer.toJoinPoolInput
                                )

                            return
                                match result with
                                | Ok _ -> Results.NoContent()
                                | Error error ->
                                    match error with
                                    | JoinPoolFailure.AlreadyJoined ->
                                        Results.BadRequest("The gambler has already joined this pool")
                                    | JoinPoolFailure.PoolNotFound -> Results.NotFound("Pool not found")
                                    | JoinPoolFailure.GamblerNotFound -> Results.NotFound("Gambler not found")
                        }
                        |> Async.StartAsTask)
                )
                .RequireAuthorization()
            |> ignore

            this
