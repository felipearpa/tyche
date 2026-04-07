namespace Felipearpa.Tyche.PoolLayout.Application

open Felipearpa.Tyche.PoolLayout.Domain

type GetOpenPoolLayouts(poolLayoutRepository: IPoolLayoutRepository) =
    member this.ExecuteAsync(maybeNext: string option) =
        poolLayoutRepository.GetOpenPoolLayoutsAsync(maybeNext)
