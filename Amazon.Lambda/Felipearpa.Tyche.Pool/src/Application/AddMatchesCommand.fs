namespace Felipearpa.Tyche.Pool.Application

open Felipearpa.Tyche.Pool.Domain

type AddMatchesCommand(poolGamblerBetRepository: IPoolGamblerBetRepository) =

    member this.ExecuteAsync(poolGamblerBets: InitialPoolGamblerBet seq) =
        poolGamblerBetRepository.AddPoolGamblerMatchesAsync(poolGamblerBets)
