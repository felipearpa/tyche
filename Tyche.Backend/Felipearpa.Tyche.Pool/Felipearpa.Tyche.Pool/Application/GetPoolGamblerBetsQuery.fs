namespace Felipearpa.Tyche.Pool.Application

open Felipearpa.Tyche.Pool.Data
open Felipearpa.Type

type GetPoolGamblerBetsQuery(poolGamblerBetRepository: IPoolGamblerBetRepository) =

    member this.ExecuteAsync(poolId: Ulid, gamblerId: Ulid, searchText: string option, next: string option) =
        poolGamblerBetRepository.GetPoolGamblerBets(poolId, gamblerId, searchText, next)
