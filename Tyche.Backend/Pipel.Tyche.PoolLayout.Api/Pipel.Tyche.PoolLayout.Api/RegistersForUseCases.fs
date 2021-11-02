namespace Pipel.Tyche.PoolLayout.Api

open Microsoft.Extensions.DependencyInjection
open Pipel.Tyche.PoolLayout.Domain.UseCases

module RegistersForUseCases =

    type IServiceCollection with

        member this.RegisterUseCases() =
            this.AddTransient<IFindActivePoolsLayoutsUseCase, FindActivePoolsLayoutsUseCase>()
