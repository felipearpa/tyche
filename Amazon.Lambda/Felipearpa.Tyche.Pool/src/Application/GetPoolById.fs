namespace Felipearpa.Tyche.Pool.Application

open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

type GetPoolById(poolRepository: IPoolRepository) =
    member this.ExecuteAsync(poolId: Ulid) = poolRepository.GetPoolByIdAsync poolId
