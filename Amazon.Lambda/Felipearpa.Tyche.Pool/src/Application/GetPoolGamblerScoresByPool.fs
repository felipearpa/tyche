namespace Felipearpa.Tyche.Pool.Application

open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

type GetPoolGamblerScoresByPool(poolGamblerScoreRepository: IPoolGamblerScoreRepository) =

    member this.ExecuteAsync(poolId: Ulid, next: string option) =
        poolGamblerScoreRepository.GetPoolScoresAsync(poolId, next)
