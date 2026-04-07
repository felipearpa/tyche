namespace Felipearpa.Tyche.Pool.Application

open Felipearpa.Tyche.Pool.Domain

type AddMatch(poolGamblerBetRepository: IPoolGamblerBetRepository) =

    member this.ExecuteAsync(poolGamblerBet: InitialPoolGamblerBet) =
        poolGamblerBetRepository.AddPoolGamblerMatchAsync(poolGamblerBet)
