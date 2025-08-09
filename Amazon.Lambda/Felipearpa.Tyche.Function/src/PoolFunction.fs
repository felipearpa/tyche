namespace Felipearpa.Tyche.Function

open System.Threading.Tasks
open Felipearpa.Core.Paging
open Felipearpa.Tyche.Function.Request
open Felipearpa.Tyche.Function.Response
open Felipearpa.Tyche.Pool.Application
open Felipearpa.Type
open Microsoft.AspNetCore.Http

module PoolFunction =

    let getPoolById (poolId: string) (getPoolByIdQuery: GetPoolByIdQuery) : IResult Async =
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

    let getGamblersByPoolId
        (poolId: string)
        (next: string option)
        (getPoolGamblerScoresByPoolQuery: GetPoolGamblerScoresByPoolQuery)
        : IResult Async =
        async {
            let! page = getPoolGamblerScoresByPoolQuery.ExecuteAsync(poolId |> Ulid.newOf, next)
            return Results.Ok(page |> CursorPage.map PoolGamblerScoreTransformer.toResponse)
        }

    let getPoolGamblerScoreById
        (poolId: string)
        (gamblerId: string)
        (getPoolGamblerScoreByIdQuery: GetPoolGamblerScoreByIdQuery)
        : IResult Async =
        async {
            let! result = getPoolGamblerScoreByIdQuery.ExecuteAsync(poolId |> Ulid.newOf, gamblerId |> Ulid.newOf)

            return
                match result with
                | Ok maybeScore ->
                    match maybeScore with
                    | None -> Results.NotFound()
                    | Some score -> Results.Ok(score |> PoolGamblerScoreTransformer.toResponse)
                | Error _ -> Results.InternalServerError()
        }

    let getPendingBets
        (poolId: string)
        (gamblerId: string)
        (searchText: string option)
        (next: string option)
        (getPendingPoolGamblerBetsQuery: GetPendingPoolGamblerBetsQuery)
        : IResult Async =
        async {
            let! page =
                getPendingPoolGamblerBetsQuery.ExecuteAsync(
                    poolId |> Ulid.newOf,
                    gamblerId |> Ulid.newOf,
                    searchText,
                    next
                )

            return Results.Ok(page |> CursorPage.map PoolGamblerBetTransformer.toResponse)
        }

    let getFinishedBets
        (poolId: string)
        (gamblerId: string)
        (searchText: string option)
        (next: string option)
        (getFinishedPoolGamblerBetsQuery: GetFinishedPoolGamblerBetsQuery)
        : IResult Async =
        async {
            let! page =
                getFinishedPoolGamblerBetsQuery.ExecuteAsync(
                    poolId |> Ulid.newOf,
                    gamblerId |> Ulid.newOf,
                    searchText,
                    next
                )

            return Results.Ok(page |> CursorPage.map PoolGamblerBetTransformer.toResponse)
        }

    let createPool (createPoolRequest: CreatePoolRequest) (createPoolCommand: CreatePoolCommand) : IResult Async =
        async {
            let! result =
                createPoolCommand.ExecuteAsync(createPoolRequest |> CreatePoolRequestTransformer.toCreatePoolInput)

            return
                match result with
                | Ok pool -> Results.Ok(pool |> CreatePoolOutputTransformer.toResponse)
                | Error _ -> Results.NotFound("Gambler not found")
        }

    let joinPool (joinPoolRequest: JoinPoolRequest) (joinPoolCommand: JoinPoolCommand) : Task<IResult> =
        async {
            let! result = joinPoolCommand.ExecuteAsync(joinPoolRequest |> JoinPoolRequestTransformer.toJoinPoolInput)

            return
                match result with
                | Ok _ -> Results.NoContent()
                | Error error ->
                    match error with
                    | JoinPoolFailure.AlreadyJoined -> Results.BadRequest("The gambler has already joined this pool")
                    | JoinPoolFailure.PoolNotFound -> Results.NotFound("Pool not found")
                    | JoinPoolFailure.GamblerNotFound -> Results.NotFound("Gambler not found")
        }
        |> Async.StartAsTask
