namespace Pipel.Tyche.Pool.Api

open Microsoft.Extensions.DependencyInjection
open Pipel.Tyche.Pool.Data

module RegistersForRepositories =

    type IServiceCollection with

        member this.RegisterRepositories() =
            this
                .AddScoped<IPoolRepository, PoolRepository>()
                .AddScoped<IPoolGamblerRepository, PoolGamblerRepository>()
                .AddScoped<IPoolGameRepository, PoolGameRepository>()
