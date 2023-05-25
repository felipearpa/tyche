module Felipearpa.Tyche.PoolLayout.Api.FindActivePoolsLayouts

open Felipearpa.Core
open Felipearpa.Data
open Felipearpa.Tyche.PoolLayout.Domain
open Felipearpa.Tyche.PoolLayout.Domain.UseCases

let execute
    (filter: string)
    (next: string)
    (findActivePoolsLayoutsUseCase: IFindActivePoolsLayoutsUseCase)
    (mapFromDomainToViewFunc: MapFunc<PoolLayout, PoolLayoutResponse>)
    =
    async {
        let! page = findActivePoolsLayoutsUseCase.AsyncExecute(filter |> String.tryOf, next |> String.tryOf)

        return page |> CursorPage.map mapFromDomainToViewFunc
    }
