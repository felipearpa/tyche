namespace Pipel.Tyche.Pool.Api

open Microsoft.Extensions.DependencyInjection
open Pipel.Data
open Pipel.Tyche.Pool
open Pipel.Tyche.Pool.Data
open Pipel.Tyche.Pool.Domain

module RegistersForMappers =

    type IServiceCollection with

        member this.RegisterMappers() =
            this
                .AddSingleton<MapFunc<PoolEntity, Pool>>(PoolDomainMapper.mapFromDomainToData)
                .AddSingleton<MapFunc<Pool, PoolResponse>>(PoolApplicationMapper.mapFromDomainToApplication)
                .AddSingleton<MapFunc<PoolGamblerEntity, PoolGambler>>(PoolGamblerDomainMapper.mapFromDomainToData)
                .AddSingleton<MapFunc<PoolGambler, PoolGamblerResponse>>(
                    PoolGamblerApplicationMapper.mapFromDomainToApplication
                )
                .AddSingleton<MapFunc<PoolGameEntity, PoolGame>>(PoolGameDomainMapper.mapFromDomainToData)
                .AddSingleton<MapFunc<PoolGame, PoolGameResponse>>(PoolGameApplicationMapper.mapFromDomainToApplication)
