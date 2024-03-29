namespace Felipearpa.Tyche.Pool.Application

open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

type GetPoolGamblerScoreQuery(poolGamblerScoreRepository: IPoolGamblerScoreRepository) =
    member this.ExecuteAsync(poolId: Ulid, gamblerId: Ulid) =
        poolGamblerScoreRepository.GetPoolGamblerScoreAsync(poolId, gamblerId)
