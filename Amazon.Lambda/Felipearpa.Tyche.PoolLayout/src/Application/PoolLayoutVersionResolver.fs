namespace Felipearpa.Tyche.PoolLayout.Application

open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.PoolLayout.Domain

type PoolLayoutVersionResolver(poolLayoutRepository: IPoolLayoutRepository) =

    interface IPoolLayoutVersionResolver with

        member this.ResolveAsync(poolLayoutId) =
            async {
                let! maybeLayout = poolLayoutRepository.GetPoolLayoutByIdAsync poolLayoutId
                return maybeLayout |> Option.map _.Version
            }
