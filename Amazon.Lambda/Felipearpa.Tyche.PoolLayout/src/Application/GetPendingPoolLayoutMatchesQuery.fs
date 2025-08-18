namespace Felipearpa.Tyche.PoolLayout.Application

open Felipearpa.Tyche.PoolLayout.Domain
open Felipearpa.Type

type GetPendingPoolLayoutMatchesQuery(poolLayoutRepository: IPoolLayoutRepository) =

    member this.ExecuteAsync(poolLayoutId: Ulid, poolLayoutVersion: int, maybeNext: string option) =
        poolLayoutRepository.GetPendingMatches(poolLayoutId, poolLayoutVersion, maybeNext)
