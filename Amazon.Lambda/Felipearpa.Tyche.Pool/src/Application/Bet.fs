namespace Felipearpa.Tyche.Pool.Application

open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Type
open Felipearpa.Type

type Bet(poolGamblerBetRepository: IPoolGamblerBetRepository) =

    member this.ExecuteAsync(poolId: Ulid, gamblerId: Ulid, matchId: Ulid, betScore: TeamScore<BetScore>) =
        poolGamblerBetRepository.BetAsync(poolId, gamblerId, matchId, betScore)
