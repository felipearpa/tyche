module Pipel.Tyche.Pool.Api.FindPoolsGamesUseCase

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
    (findPoolsGamesUseCase: IFindPoolsGamesUseCase)
    (mapFromDomainToViewFunc: MapFunc<PoolGame, PoolGameResponse>)
    =
    async {
        let! page =
            findPoolsGamesUseCase.AsyncExecute(
                { PoolPK.PoolId = Ulid.From poolId },
                filter |> String.tryFrom,
                next |> String.tryFrom
            )

        return page |> CursorPage.map mapFromDomainToViewFunc
    }
