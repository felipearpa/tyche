namespace Felipearpa.Tyche.Pool.Application

open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Type
open Felipearpa.Type

type ComputeBetsCommand(poolGamblerScoreRepository: IPoolGamblerScoreRepository) =

    member this.ExecuteAsync(matchId: Ulid, matchScore: TeamScore<int>) =
        poolGamblerScoreRepository.Compute(matchId, matchScore)
