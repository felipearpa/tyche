module Pipel.Tyche.Pool.Api.FindPools

open Pipel.Core
open Pipel.Data
open Pipel.Tyche.Pool.Domain
open Pipel.Tyche.Pool.Domain.UseCases
open Pipel.Type

let execute
    (poolLayoutId: string)
    (filter: string)
    (next: string)
    (findPoolsUseCase: IFindPoolsUseCase)
    (mapFromDomainToViewFunc: MapFunc<Pool, PoolResponse>)
    =
    async {
        let! page =
            findPoolsUseCase.AsyncExecute(
                { PoolLayoutPK.PoolLayoutId = Ulid.From poolLayoutId },
                filter |> String.tryFrom,
                next |> String.tryFrom
            )

        return page |> CursorPage.map mapFromDomainToViewFunc
    }
