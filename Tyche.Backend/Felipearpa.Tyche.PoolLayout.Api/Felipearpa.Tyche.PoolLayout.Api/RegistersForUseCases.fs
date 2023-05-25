namespace Felipearpa.Tyche.PoolLayout.Api

open Microsoft.Extensions.DependencyInjection
open Felipearpa.Tyche.PoolLayout.Domain.UseCases

module RegistersForUseCases =

    type IServiceCollection with

        member this.RegisterUseCases() =
            this.AddTransient<IFindActivePoolsLayoutsUseCase, FindActivePoolsLayoutsUseCase>()
