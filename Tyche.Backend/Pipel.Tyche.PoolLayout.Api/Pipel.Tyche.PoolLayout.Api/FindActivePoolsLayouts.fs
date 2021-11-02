module Pipel.Tyche.PoolLayout.Api.FindActivePoolsLayouts

open System
open Pipel.Core
open Pipel.Data
open Pipel.Tyche.PoolLayout.Api.Validation
open Pipel.Tyche.PoolLayout.Domain
open Pipel.Tyche.PoolLayout.Domain.UseCases
open Pipel.Type

let execute
    (filter: string)
    (skip: int)
    (take: int)
    (findActivePoolsLayoutsUseCase: IFindActivePoolsLayoutsUseCase)
    (mapFromDomainToViewFunc: MapFunc<PoolLayout, PoolLayoutResponse>)
    =
    async {
        match isSkip skip with
        | Error message -> ArgumentException(message) |> raise
        | _ -> ()

        match isTake take with
        | Error message -> ArgumentException(message) |> raise
        | _ -> ()

        let! page = findActivePoolsLayoutsUseCase.AsyncExecute(NonEmptyString.TryFrom filter, skip, take)

        return page |> Page.map mapFromDomainToViewFunc
    }
