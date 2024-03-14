namespace Felipearpa.Tyche.Pool.Application

open Felipearpa.Tyche.Pool.Data
open Felipearpa.Type

type GetFinishedPoolGamblerBetsQuery(poolGamblerBetRepository: IPoolGamblerBetRepository) =

    member this.ExecuteAsync(poolId: Ulid, gamblerId: Ulid, searchText: string option, next: string option) =
        poolGamblerBetRepository.GetFinishedPoolGamblerBets(poolId, gamblerId, searchText, next)
