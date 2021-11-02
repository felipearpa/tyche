module Pipel.Tyche.PoolLayout.Api.WebApplicationInit

open System
open Microsoft.AspNetCore.Builder
open Microsoft.Extensions.Hosting
open Pipel.Data
open Pipel.Tyche.PoolLayout.Domain
open Pipel.Tyche.PoolLayout.Domain.UseCases

type WebApplication with

    member this.Init() =
        if this.Environment.IsDevelopment() then
            this.UseDeveloperExceptionPage() |> ignore

        this.MapGet(
            "/poolLayout/findActivePoolsLayouts",
            Func<_, _, _, _, _, _>
                (fun (filter: string) (skip: int) (take: int) (findActivePoolsLayoutsUseCase: IFindActivePoolsLayoutsUseCase) (mapFromDomainToViewFunc: MapFunc<PoolLayout, PoolLayoutResponse>) ->
                    FindActivePoolsLayouts.execute
                        filter
                        skip
                        take
                        findActivePoolsLayoutsUseCase
                        mapFromDomainToViewFunc
                    |> Async.StartAsTask)
        )
        |> ignore
