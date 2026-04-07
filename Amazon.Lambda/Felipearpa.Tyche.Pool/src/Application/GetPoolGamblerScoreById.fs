namespace Felipearpa.Tyche.Pool.Application

open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

type GetPoolGamblerScoreById(poolGamblerScoreRepository: IPoolGamblerScoreRepository) =
    member this.ExecuteAsync(poolId: Ulid, gamblerId: Ulid) =
        poolGamblerScoreRepository.GetPoolGamblerScoreByIdAsync(poolId, gamblerId)
