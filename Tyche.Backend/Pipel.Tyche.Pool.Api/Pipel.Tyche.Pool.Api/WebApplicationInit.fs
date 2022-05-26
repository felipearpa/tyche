module Pipel.Tyche.Pool.Api.WebApplicationInit

open System
open Microsoft.AspNetCore.Builder
open Microsoft.Extensions.Hosting
open Pipel.Data
open Pipel.Tyche.Pool.Domain
open Pipel.Tyche.Pool.Domain.UseCases

type WebApplication with

    member this.Init() =
        if this.Environment.IsDevelopment() then
            this.UseDeveloperExceptionPage() |> ignore

        this.MapGet(
            "/pool/findPools",
            Func<_, _, _, _, _, _>
                (fun (poolLayoutId: string) (filterText: string) (nextToken: string) (findPoolsUseCase: IFindPoolsUseCase) (mapFromDomainToViewFunc: MapFunc<Pool, PoolResponse>) ->
                    FindPools.execute poolLayoutId filterText nextToken findPoolsUseCase mapFromDomainToViewFunc
                    |> Async.StartAsTask)
        )
        |> ignore
