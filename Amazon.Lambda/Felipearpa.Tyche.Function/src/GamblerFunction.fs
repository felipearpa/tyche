namespace Felipearpa.Tyche.Function

open System.Threading.Tasks
open Amazon.Lambda.Annotations.APIGateway
open Felipearpa.Core.Paging
open Felipearpa.Tyche.Function.Response
open Felipearpa.Tyche.Pool.Application
open Felipearpa.Type

type GamblerFunction() =

    member this.GetPoolsByGamblerId
        (
            gamblerId: string,
            next: string,
            getPoolGamblerScoresByGamblerQuery: GetPoolGamblerScoresByGamblerQuery
        ) : Task<IHttpResult> =
        async {
            let! page = getPoolGamblerScoresByGamblerQuery.ExecuteAsync(gamblerId |> Ulid.newOf, next |> Option.ofObj)

            return HttpResults.Ok(page |> CursorPage.map PoolGamblerScoreTransformer.toResponse)
        }
        |> Async.StartAsTask
