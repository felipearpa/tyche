module Felipearpa.Tyche.PoolLayout.Api.WebApplicationInit

open System
open Microsoft.AspNetCore.Builder
open Microsoft.Extensions.Hosting
open Felipearpa.Data
open Felipearpa.Tyche.PoolLayout.Domain
open Felipearpa.Tyche.PoolLayout.Domain.UseCases

type WebApplication with

    member this.Init() =
        if this.Environment.IsDevelopment() then
            this.UseDeveloperExceptionPage() |> ignore

        this.MapGet(
            "/poolLayout/findActivePoolsLayouts",
            Func<_, _, _, _, _>
                (fun (filterText: string) (nextToken: string) (findActivePoolsLayoutsUseCase: IFindActivePoolsLayoutsUseCase) (mapFromDomainToViewFunc: MapFunc<PoolLayout, PoolLayoutResponse>) ->
                    FindActivePoolsLayouts.execute
                        filterText
                        nextToken
                        findActivePoolsLayoutsUseCase
                        mapFromDomainToViewFunc
                    |> Async.StartAsTask)
        )
        |> ignore
