namespace Felipearpa.Tyche.Function

open System.Threading.Tasks
open Amazon.Lambda.Annotations.APIGateway
open Felipearpa.Core.Paging
open Felipearpa.Tyche.Function.Response
open Felipearpa.Tyche.PoolLayout.Application

type PoolLayoutFunction() =

    member this.GetOpenPoolLayouts
        (
            next: string,
            getOpenedPoolLayoutsQuery: GetOpenPoolLayoutsQuery
        ) : Task<IHttpResult> =
        async {
            let! page = getOpenedPoolLayoutsQuery.ExecuteAsync(next |> Option.ofObj)

            return HttpResults.Ok(page |> CursorPage.map PoolLayoutTransformer.toPoolLayoutResponse)
        }
        |> Async.StartAsTask
