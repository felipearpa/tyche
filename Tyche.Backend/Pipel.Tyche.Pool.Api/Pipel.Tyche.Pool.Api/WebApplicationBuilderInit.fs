module Pipel.Tyche.Pool.Api.WebApplicationBuilderInit

open Microsoft.AspNetCore.Builder
open Pipel.Tyche.Pool.Api.RegistersForDatabases
open Pipel.Tyche.Pool.Api.RegistersForRepositories
open Pipel.Tyche.Pool.Api.RegistersForUseCases
open Pipel.Tyche.Pool.Api.RegistersForMappers
open Pipel.Tyche.Pool.Api.RegistersForHelpers

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
