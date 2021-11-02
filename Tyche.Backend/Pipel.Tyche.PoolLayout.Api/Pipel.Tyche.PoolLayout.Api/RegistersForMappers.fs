namespace Pipel.Tyche.PoolLayout.Api

open Microsoft.Extensions.DependencyInjection
open Pipel.Data
open Pipel.Tyche.PoolLayout
open Pipel.Tyche.PoolLayout.Data
open Pipel.Tyche.PoolLayout.Domain

module RegistersForMappers =

    type IServiceCollection with

        member this.RegisterMappers() =
            this
                .AddSingleton<MapFunc<PoolLayoutEntity, PoolLayout>>(PoolLayoutDomainMapper.mapFromDomainToData)
                .AddSingleton<MapFunc<PoolLayout, PoolLayoutResponse>>(
                    PoolLayoutApplicationMapper.mapFromDomainToApplication
                )
