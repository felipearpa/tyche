namespace Felipearpa.Tyche.Pool.Application

open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

type UpdatePositions(poolGamblerScoreRepository: IPoolGamblerScoreRepository) =

    member this.ExecuteAsync(poolId: Ulid, matchId: Ulid) =
        async { do! poolGamblerScoreRepository.UpdatePositions(poolId, matchId) }
