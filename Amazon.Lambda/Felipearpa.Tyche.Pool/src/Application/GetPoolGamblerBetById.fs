namespace Felipearpa.Tyche.Pool.Application

open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

type GetPoolGamblerBetById(poolGamblerBetRepository: IPoolGamblerBetRepository) =
    member this.ExecuteAsync(poolId: Ulid, gamblerId: Ulid, matchId: Ulid) =
        poolGamblerBetRepository.GetPoolGamblerBetByIdAsync(poolId, gamblerId, matchId)
