namespace Felipearpa.Tyche.Function

open Felipearpa.Core.Paging
open Felipearpa.Tyche.Function.Response
open Felipearpa.Tyche.PoolLayout.Application
open Microsoft.AspNetCore.Http

module PoolLayoutFunction =

    let getOpenPoolLayouts (next: string option) (getOpenedPoolLayoutsQuery: GetOpenPoolLayoutsQuery) : IResult Async =
        async {
            let! page = getOpenedPoolLayoutsQuery.ExecuteAsync next
            return Results.Ok(page |> CursorPage.map PoolLayoutTransformer.toPoolLayoutResponse)
        }
