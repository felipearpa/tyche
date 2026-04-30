namespace Felipearpa.Tyche.Pool.Application

open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

type GetPoolMatchGamblerBets(poolGamblerBetRepository: IPoolGamblerBetRepository) =

    member this.ExecuteAsync(poolId: Ulid, matchId: Ulid, next: string option) =
        poolGamblerBetRepository.GetPoolMatchGamblerBetsAsync(poolId, matchId, next)
