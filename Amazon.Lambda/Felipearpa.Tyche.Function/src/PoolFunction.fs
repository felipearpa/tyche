namespace Felipearpa.Tyche.Function

open Felipearpa.Core.Paging
open Felipearpa.Tyche.Function.Request
open Felipearpa.Tyche.Function.Response
open Felipearpa.Tyche.Pool.Application
open Felipearpa.Type
open Microsoft.AspNetCore.Http

module PoolFunction =

    let getPoolByIdAsync (poolId: string) (getPoolById: GetPoolById) : IResult Async =
        async {
            let! result = getPoolById.ExecuteAsync(poolId |> Ulid.newOf)

            return
                match result with
                | Ok maybePool ->
                    match maybePool with
                    | None -> Results.NotFound("Pool not found")
                    | Some pool -> Results.Ok(pool |> PoolTransformer.toPoolViewModel)
                | Error _ -> Results.InternalServerError()
        }

    let getGamblersByPoolIdAsync
        (poolId: string)
        (next: string option)
        (getPoolGamblerScoresByPool: GetPoolGamblerScoresByPool)
        : IResult Async =
        async {
            let! page = getPoolGamblerScoresByPool.ExecuteAsync(poolId |> Ulid.newOf, next)
            return Results.Ok(page |> CursorPage.map PoolGamblerScoreTransformer.toResponse)
        }

    let getPoolGamblerScoreByIdAsync
        (poolId: string)
        (gamblerId: string)
        (getPoolGamblerScoreById: GetPoolGamblerScoreById)
        : IResult Async =
        async {
            let! result = getPoolGamblerScoreById.ExecuteAsync(poolId |> Ulid.newOf, gamblerId |> Ulid.newOf)

            return
                match result with
                | Ok maybeScore ->
                    match maybeScore with
                    | None -> Results.NotFound()
                    | Some score -> Results.Ok(score |> PoolGamblerScoreTransformer.toResponse)
                | Error _ -> Results.InternalServerError()
        }

    let getPendingBetsAsync
        (poolId: string)
        (gamblerId: string)
        (searchText: string option)
        (next: string option)
        (getPendingPoolGamblerBets: GetPendingPoolGamblerBets)
        : IResult Async =
        async {
            let! page =
                getPendingPoolGamblerBets.ExecuteAsync(poolId |> Ulid.newOf, gamblerId |> Ulid.newOf, searchText, next)

            return Results.Ok(page |> CursorPage.map PoolGamblerBetTransformer.toResponse)
        }

    let getFinishedBetsAsync
        (poolId: string)
        (gamblerId: string)
        (searchText: string option)
        (next: string option)
        (getFinishedPoolGamblerBets: GetFinishedPoolGamblerBets)
        : IResult Async =
        async {
            let! page =
                getFinishedPoolGamblerBets.ExecuteAsync(poolId |> Ulid.newOf, gamblerId |> Ulid.newOf, searchText, next)

            return Results.Ok(page |> CursorPage.map PoolGamblerBetTransformer.toResponse)
        }

    let getLiveBetsAsync
        (poolId: string)
        (gamblerId: string)
        (searchText: string option)
        (next: string option)
        (getLivePoolGamblerBets: GetLivePoolGamblerBets)
        : IResult Async =
        async {
            let! page =
                getLivePoolGamblerBets.ExecuteAsync(poolId |> Ulid.newOf, gamblerId |> Ulid.newOf, searchText, next)

            return Results.Ok(page |> CursorPage.map PoolGamblerBetTransformer.toResponse)
        }

    let getMatchBetsAsync
        (poolId: string)
        (matchId: string)
        (next: string option)
        (getPoolMatchGamblerBets: GetPoolMatchGamblerBets)
        : IResult Async =
        async {
            let! page = getPoolMatchGamblerBets.ExecuteAsync(poolId |> Ulid.newOf, matchId |> Ulid.newOf, next)
            return Results.Ok(page |> CursorPage.map PoolGamblerBetTransformer.toResponse)
        }

    let createPoolAsync (createPoolRequest: CreatePoolRequest) (createPool: CreatePool) : IResult Async =
        async {
            let! result = createPool.ExecuteAsync(createPoolRequest |> CreatePoolRequestTransformer.toCreatePoolInput)

            return
                match result with
                | Ok pool -> Results.Ok(pool |> CreatePoolOutputTransformer.toResponse)
                | Error _ -> Results.NotFound("Gambler not found")
        }

    let joinPoolAsync (poolId: string) (joinPoolRequest: JoinPoolRequest) (joinPool: JoinPool) : IResult Async =
        async {
            let! result = joinPool.ExecuteAsync(joinPoolRequest |> JoinPoolRequestTransformer.toJoinPoolInput poolId)

            return
                match result with
                | Ok _ -> Results.NoContent()
                | Error error ->
                    match error with
                    | JoinPoolFailure.AlreadyJoined -> Results.BadRequest("The gambler has already joined this pool")
                    | JoinPoolFailure.PoolNotFound -> Results.NotFound("Pool not found")
                    | JoinPoolFailure.GamblerNotFound -> Results.NotFound("Gambler not found")
        }
