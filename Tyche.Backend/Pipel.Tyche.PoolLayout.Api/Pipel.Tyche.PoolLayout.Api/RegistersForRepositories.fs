namespace Pipel.Tyche.PoolLayout.Api

open Microsoft.Extensions.DependencyInjection
open Pipel.Tyche.PoolLayout.Data

module RegistersForRepositories =

    type IServiceCollection with

        member this.RegisterRepositories() =
            this.AddScoped<IPoolLayoutRepository, PoolLayoutRepository>()
