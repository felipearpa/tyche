namespace Felipearpa.Tyche.PoolLayout.Api

open Microsoft.Extensions.DependencyInjection
open Felipearpa.Core
open Felipearpa.Core.Json

module RegistersForHelpers =

    type IServiceCollection with

        member this.RegisterHelpers() =
            this.AddSingleton<ISerializer, DefaultJsonSerializer>()
