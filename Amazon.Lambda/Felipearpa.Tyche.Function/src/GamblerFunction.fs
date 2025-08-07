namespace Felipearpa.Tyche.Function

open Felipearpa.Core.Paging
open Felipearpa.Tyche.Function.Response
open Felipearpa.Tyche.Pool.Application
open Felipearpa.Type
open Microsoft.AspNetCore.Http

module GamblerFunction =

    let getPoolsByGamblerId
        (gamblerId: string)
        (next: string option)
        (getPoolGamblerScoresByGamblerQuery: GetPoolGamblerScoresByGamblerQuery)
        : IResult Async =
        async {
            let! page = getPoolGamblerScoresByGamblerQuery.ExecuteAsync(gamblerId |> Ulid.newOf, next)

            return Results.Ok(page |> CursorPage.map PoolGamblerScoreTransformer.toResponse)
        }
