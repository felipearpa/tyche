namespace Felipearpa.Tyche.Pool.Application

open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

type GetPoolGamblerScoresByGambler(poolGamblerScoreRepository: IPoolGamblerScoreRepository) =

    member this.ExecuteAsync(gamblerId: Ulid, next: string option) =
        poolGamblerScoreRepository.GetGamblerScoresAsync(gamblerId, next)
