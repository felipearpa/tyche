namespace Pipel.Tyche.Pool.Api

open Microsoft.Extensions.DependencyInjection
open Pipel.Tyche.Pool.Domain.UseCases

module RegistersForUseCases =

    type IServiceCollection with

        member this.RegisterUseCases() =
            this
                .AddTransient<IFindPoolsUseCase, FindPoolsUseCase>()
                .AddTransient<IFindPoolsGamblersUseCase, FindPoolsGamblersUseCase>()
