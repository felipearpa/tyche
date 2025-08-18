namespace Felipearpa.Tyche.Pool.Application

open Felipearpa.Tyche.Pool.Domain

type AddMatchCommand(poolGamblerBetRepository: IPoolGamblerBetRepository) =

    member this.ExecuteAsync(poolGamblerBet: InitialPoolGamblerBet) =
        poolGamblerBetRepository.AddMatch(poolGamblerBet)
