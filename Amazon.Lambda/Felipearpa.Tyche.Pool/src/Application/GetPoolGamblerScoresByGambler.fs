namespace Felipearpa.Tyche.Pool.Application

open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

type GetPoolGamblerScoresByGambler(poolGamblerScoreRepository: IPoolGamblerScoreRepository, poolStandings: PoolStandings) =

    member this.ExecuteAsync(gamblerId: Ulid, next: string option) =
        async {
            let! page = poolGamblerScoreRepository.GetGamblerScoresAsync(gamblerId, next)
            return! poolStandings.WithTrendGroupsAsync page
        }
