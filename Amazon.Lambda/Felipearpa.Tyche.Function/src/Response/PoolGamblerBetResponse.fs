namespace Felipearpa.Tyche.Function.Response

open System

type PoolGamblerBetResponse =
    { PoolId: string
      GamblerId: string
      MatchId: string
      HomeTeamId: string
      HomeTeamName: string
      HomeTeamScore: int option
      HomeTeamBet: int option
      AwayTeamId: string
      AwayTeamName: string
      AwayTeamScore: int option
      AwayTeamBet: int option
      Score: int option
      MatchDateTime: DateTime
      isLocked: bool }
