namespace Felipearpa.Tyche.Pool.Application

open Felipearpa.Core
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Type
open Felipearpa.Type

type ComputeBetsCommand(poolGamblerScoreRepository: IPoolGamblerScoreRepository) =

    member this.ExecuteAsync(matchId: Ulid, matchScore: TeamScore<int>) =
        async {
            let! affectedPoolIds = poolGamblerScoreRepository.Compute(matchId, matchScore)

            do!
                affectedPoolIds
                |> Seq.map poolGamblerScoreRepository.UpdatePositions
                |> Seq.iterAsync id
        }
