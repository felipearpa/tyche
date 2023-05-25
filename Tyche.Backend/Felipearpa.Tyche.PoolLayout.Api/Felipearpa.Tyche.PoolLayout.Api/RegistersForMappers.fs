namespace Felipearpa.Tyche.PoolLayout.Api

open Microsoft.Extensions.DependencyInjection
open Felipearpa.Data
open Felipearpa.Tyche.PoolLayout
open Felipearpa.Tyche.PoolLayout.Data
open Felipearpa.Tyche.PoolLayout.Domain

module RegistersForMappers =

    type IServiceCollection with

        member this.RegisterMappers() =
            this
                .AddSingleton<MapFunc<PoolLayoutEntity, PoolLayout>>(PoolLayoutDomainMapper.mapFromDomainToData)
                .AddSingleton<MapFunc<PoolLayout, PoolLayoutResponse>>(
                    PoolLayoutApplicationMapper.mapFromDomainToApplication
                )
