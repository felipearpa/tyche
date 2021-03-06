module Pipel.Tyche.PoolLayout.Api.WebApplicationBuilderInit

open Microsoft.AspNetCore.Builder
open Pipel.Tyche.PoolLayout.Api.RegistersForDatabases
open Pipel.Tyche.PoolLayout.Api.RegistersForRepositories
open Pipel.Tyche.PoolLayout.Api.RegistersForUseCases
open Pipel.Tyche.PoolLayout.Api.RegistersForMappers
open Pipel.Tyche.PoolLayout.Api.RegistersForHelpers

type WebApplicationBuilder with

    member this.Init() =
        this
            .Services
            .RegisterDatabases(this.Configuration)
            .RegisterRepositories()
            .RegisterUseCases()
            .RegisterMappers()
            .RegisterHelpers()
        |> ignore
