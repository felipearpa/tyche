namespace Felipearpa.Tyche.PoolLayout.Api

open System
open Felipearpa.Core.Paging
open Felipearpa.Tyche.PoolLayout.Application
open Microsoft.AspNetCore.Builder

[<AutoOpen>]
module WebApplication =

    type WebApplication with

        member this.ConfigureMiddleware() =
            this.UseAuthentication().UseAuthorization() |> ignore

            this

        member this.ConfigureRoutes() =
            this
                .MapGet(
                    "/pool-layouts/opened",
                    Func<_, _, _>(fun (next: string) (getOpenedPoolLayoutsQuery: GetOpenedPoolLayoutsQuery) ->
                        async {
                            let! page = getOpenedPoolLayoutsQuery.ExecuteAsync(next |> Option.ofObj)
                            return page |> CursorPage.map PoolLayoutTransformer.toPoolLayoutResponse
                        }
                        |> Async.StartAsTask

                    )
                )
                .RequireAuthorization()
            |> ignore

            this
