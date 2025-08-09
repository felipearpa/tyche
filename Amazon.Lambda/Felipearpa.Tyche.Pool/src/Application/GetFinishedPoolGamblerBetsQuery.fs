namespace Felipearpa.Tyche.Pool.Application

open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

type GetFinishedPoolGamblerBetsQuery(poolGamblerBetRepository: IPoolGamblerBetRepository) =

    member this.ExecuteAsync(poolId: Ulid, gamblerId: Ulid, searchText: string option, next: string option) =
        poolGamblerBetRepository.GetFinishedPoolGamblerBetsAsync(poolId, gamblerId, searchText, next)
