module Felipearpa.Tyche.PoolLayout.Api.WebApplicationBuilderInit

open Microsoft.AspNetCore.Builder
open Felipearpa.Tyche.PoolLayout.Api.RegistersForDatabases
open Felipearpa.Tyche.PoolLayout.Api.RegistersForRepositories
open Felipearpa.Tyche.PoolLayout.Api.RegistersForUseCases
open Felipearpa.Tyche.PoolLayout.Api.RegistersForMappers
open Felipearpa.Tyche.PoolLayout.Api.RegistersForHelpers

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
