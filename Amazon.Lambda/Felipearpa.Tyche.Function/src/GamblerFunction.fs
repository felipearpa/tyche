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
