namespace Pipel.Tyche.Pool.Api

open Microsoft.Extensions.DependencyInjection
open Pipel.Core
open Pipel.Core.Json

module RegistersForHelpers =

    type IServiceCollection with

        member this.RegisterHelpers() =
            this.AddSingleton<ISerializer, DefaultJsonSerializer>()
