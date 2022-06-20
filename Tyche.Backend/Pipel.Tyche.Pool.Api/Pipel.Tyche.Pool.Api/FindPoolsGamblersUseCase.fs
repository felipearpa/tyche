module Pipel.Tyche.Pool.Api.FindPoolsGamblersUseCase

open Pipel.Core
open Pipel.Data
open Pipel.Tyche.Pool.Api
open Pipel.Tyche.Pool.Domain
open Pipel.Tyche.Pool.Domain.UseCases
open Pipel.Type

let execute
    (poolId: string)
    (filter: string)
    (next: string)
    (findPoolsGamblersUseCase: IFindPoolsGamblersUseCase)
    (mapFromDomainToViewFunc: MapFunc<PoolGambler, PoolGamblerResponse>)
    =
    async {
        let! page =
            findPoolsGamblersUseCase.AsyncExecute(
                { PoolPK.PoolId = Ulid.From poolId },
                filter |> String.tryFrom,
                next |> String.tryFrom
            )

        return page |> CursorPage.map mapFromDomainToViewFunc
    }
