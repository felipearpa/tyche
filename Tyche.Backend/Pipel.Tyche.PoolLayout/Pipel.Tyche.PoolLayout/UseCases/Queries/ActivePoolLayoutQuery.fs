namespace Pipel.Tyche.PoolLayout.Domain.UseCases.Queries

open System
open System.Linq
open Pipel.Tyche.PoolLayout.Data
open Pipel.Type

module ActivePoolLayoutQuery =

    let execute (filterText: NonEmptyString option) (sourceQuery: IQueryable<PoolLayoutEntity>) =
        let mutable outQuery = sourceQuery
        let now = DateTime.UtcNow

        outQuery <-
            outQuery.Where
                (fun item ->
                    now >= item.OpeningStartDateTime
                    && now < item.OpeningEndDateTime)

        outQuery <-
            match filterText with
            | Some it ->
                match it |> NonEmptyString.value with
                | value -> outQuery.Where(fun element -> element.Name.ToLower().Contains(value.ToLower()))
            | None -> outQuery

        outQuery
            .OrderByDescending(fun it -> it.OpeningStartDateTime)
            .ThenBy(fun it -> it.Name)
        :> IQueryable<PoolLayoutEntity>
