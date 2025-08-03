namespace Felipearpa.Tyche.Pool.Application

open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

type GetPoolGamblerScoresByPoolQuery(poolGamblerScoreRepository: IPoolGamblerScoreRepository) =

    member this.ExecuteAsync(poolId: Ulid, next: string option) =
        poolGamblerScoreRepository.GetPoolScoresAsync(poolId, next)
