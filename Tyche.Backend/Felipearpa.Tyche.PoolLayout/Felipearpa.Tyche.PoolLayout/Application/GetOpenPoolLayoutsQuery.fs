namespace Felipearpa.Tyche.PoolLayout.Application

open Felipearpa.Tyche.PoolLayout.Domain

type GetOpenPoolLayoutsQuery(poolLayoutRepository: IPoolLayoutRepository) =
    member this.ExecuteAsync(maybeNext: string option) =
        poolLayoutRepository.GetOpenPoolLayoutsAsync(maybeNext)
