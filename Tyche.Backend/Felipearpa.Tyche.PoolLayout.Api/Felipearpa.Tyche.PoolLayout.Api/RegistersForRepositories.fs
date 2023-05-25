namespace Felipearpa.Tyche.PoolLayout.Api

open Microsoft.Extensions.DependencyInjection
open Felipearpa.Tyche.PoolLayout.Data

module RegistersForRepositories =

    type IServiceCollection with

        member this.RegisterRepositories() =
            this.AddScoped<IPoolLayoutRepository, PoolLayoutRepository>()
