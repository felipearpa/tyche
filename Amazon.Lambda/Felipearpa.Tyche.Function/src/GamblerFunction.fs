namespace Felipearpa.Tyche.Function

open Felipearpa.Core.Paging
open Felipearpa.Tyche.Function.Response
open Felipearpa.Tyche.Pool.Application
open Felipearpa.Type
open Microsoft.AspNetCore.Http

module GamblerFunction =

    let getPoolsByGamblerIdAsync
        (gamblerId: string)
        (next: string option)
        (getPoolGamblerScoresByGambler: GetPoolGamblerScoresByGambler)
        : IResult Async =
        async {
            let! page = getPoolGamblerScoresByGambler.ExecuteAsync(gamblerId |> Ulid.newOf, next)

            return Results.Ok(page |> CursorPage.map PoolGamblerScoreTransformer.toResponse)
        }

    let getPoolMembersAsync
        (poolId: string)
        (callerGamblerId: Ulid)
        (next: string option)
        (getPoolMembers: GetPoolMembers)
        : IResult Async =
        async {
            let! result = getPoolMembers.ExecuteAsync(poolId |> Ulid.newOf, callerGamblerId, next)

            return
                match result with
                | Ok page -> Results.Ok(page |> CursorPage.map PoolMemberTransformer.toResponse)
                | Error GetPoolMembersFailure.PoolNotFound -> Results.NotFound("Pool not found")
                | Error GetPoolMembersFailure.NotOwner -> Results.Forbid()
        }

    let removeGamblerAsync
        (poolId: string)
        (gamblerId: string)
        (callerGamblerId: Ulid)
        (removeGambler: RemovePoolGambler)
        : IResult Async =
        async {
            let! result = removeGambler.ExecuteAsync(poolId |> Ulid.newOf, callerGamblerId, gamblerId |> Ulid.newOf)

            return
                match result with
                | Ok _ -> Results.NoContent()
                | Error RemovePoolGamblerFailure.PoolNotFound -> Results.NotFound("Pool not found")
                | Error RemovePoolGamblerFailure.NotOwner -> Results.Forbid()
                | Error RemovePoolGamblerFailure.CannotRemoveOwner ->
                    Results.Conflict("The pool owner cannot be removed")
        }
