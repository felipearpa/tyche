namespace Felipearpa.Tyche.Pool.Application

open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

type GetPoolQuery(poolRepository: IPoolRepository) =

    member this.ExecuteAsync(poolId: Ulid) = poolRepository.GetPoolAsync(poolId)
