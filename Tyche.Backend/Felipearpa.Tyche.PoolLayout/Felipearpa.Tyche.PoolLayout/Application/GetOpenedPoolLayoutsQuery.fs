namespace Felipearpa.Tyche.PoolLayout.Application

open Felipearpa.Tyche.PoolLayout.Domain

type GetOpenedPoolLayoutsQuery(poolLayoutRepository: IPoolLayoutRepository) =
    member this.ExecuteAsync(maybeNext: string option) =
        poolLayoutRepository.GetOpenedPoolLayoutsAsync(maybeNext)
