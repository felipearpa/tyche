namespace Felipearpa.Tyche.Pool.Application

open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

type GetPoolGamblerScoresByGamblerQuery(poolGamblerScoreRepository: IPoolGamblerScoreRepository) =

    member this.ExecuteAsync(gamblerId: Ulid, next: string option) =
        poolGamblerScoreRepository.GetGamblerScoresAsync(gamblerId, next)
