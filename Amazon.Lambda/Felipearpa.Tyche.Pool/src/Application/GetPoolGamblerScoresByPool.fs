namespace Felipearpa.Tyche.Pool.Application

open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

type GetPoolGamblerScoresByPool(poolGamblerScoreRepository: IPoolGamblerScoreRepository, poolStandings: PoolStandings) =

    member this.ExecuteAsync(poolId: Ulid, next: string option) =
        async {
            let! page = poolGamblerScoreRepository.GetPoolScoresAsync(poolId, next)
            return! poolStandings.WithTrendGroupsAsync page
        }
