namespace Felipearpa.Tyche.Pool.Application

open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Type
open Felipearpa.Type

type ComputeBetsCommand
    (poolGamblerScoreRepository: IPoolGamblerScoreRepository, updatePositionsPublisher: IUpdatePositionsPublisher) =

    member this.ExecuteAsync(matchId: Ulid, matchScore: TeamScore<int>) =
        async {
            do!
                poolGamblerScoreRepository.Compute(
                    matchId,
                    matchScore,
                    fun poolId -> updatePositionsPublisher.Publish(poolId, matchId)
                )
        }
