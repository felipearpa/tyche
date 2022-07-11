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
                    FindPoolsUseCase.execute poolLayoutId filterText nextToken findPoolsUseCase mapFromDomainToViewFunc
                    |> Async.StartAsTask)
        )
        |> ignore

        this.MapGet(
            "/pool/findPoolsGamblers",
            Func<_, _, _, _, _, _>
                (fun (poolId: string) (filterText: string) (nextToken: string) (findPoolsGamblersUseCase: IFindPoolsGamblersUseCase) (mapFromDomainToViewFunc: MapFunc<PoolGambler, PoolGamblerResponse>) ->
                    FindPoolsGamblersUseCase.execute
                        poolId
                        filterText
                        nextToken
                        findPoolsGamblersUseCase
                        mapFromDomainToViewFunc
                    |> Async.StartAsTask)
        )
        |> ignore

        this.MapGet(
            "/pool/findPoolsGames",
            Func<_, _, _, _, _, _>
                (fun (poolId: string) (filterText: string) (nextToken: string) (findPoolsGamesUseCase: IFindPoolsGamesUseCase) (mapFromDomainToViewFunc: MapFunc<PoolGame, PoolGameResponse>) ->
                    FindPoolsGamesUseCase.execute
                        poolId
                        filterText
                        nextToken
                        findPoolsGamesUseCase
                        mapFromDomainToViewFunc
                    |> Async.StartAsTask)
        )
        |> ignore
