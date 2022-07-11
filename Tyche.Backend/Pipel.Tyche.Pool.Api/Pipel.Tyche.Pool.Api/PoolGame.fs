namespace Pipel.Tyche.Pool.Api

open System

type PoolGameResponse =
    { PoolId: string
      GameId: string
      HomeTeamId: string
      HomeTeamName: string
      HomeTeamScore: Nullable<int>
      HomeTeamBet: Nullable<int>
      AwayTeamId: string
      AwayTeamName: string
      AwayTeamScore: Nullable<int>
      AwayTeamBet: Nullable<int>
      BetScore: Nullable<int>
      MatchDateTime: DateTime }
