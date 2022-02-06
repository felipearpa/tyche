module Pipel.Tyche.PoolLayout.Api.FindActivePoolsLayouts

open Pipel.Core
open Pipel.Data
open Pipel.Tyche.PoolLayout.Domain
open Pipel.Tyche.PoolLayout.Domain.UseCases
open Pipel.Type

let execute
    (filter: string)
    (next: string)
    (findActivePoolsLayoutsUseCase: IFindActivePoolsLayoutsUseCase)
    (mapFromDomainToViewFunc: MapFunc<PoolLayout, PoolLayoutResponse>)
    =
    async {
        let! page = findActivePoolsLayoutsUseCase.AsyncExecute(filter |> String.tryFrom, next |> String.tryFrom)

        return page |> CursorPage.map mapFromDomainToViewFunc
    }
