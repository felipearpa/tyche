namespace Felipearpa.Tyche.Pool.Api.ViewModel

open System

type PoolGamblerBetViewModel =
    { PoolId: string
      GamblerId: string
      MatchId: string
      HomeTeamId: string
      HomeTeamName: string
      HomeTeamScore: Nullable<int>
      HomeTeamBet: Nullable<int>
      AwayTeamId: string
      AwayTeamName: string
      AwayTeamScore: Nullable<int>
      AwayTeamBet: Nullable<int>
      Score: Nullable<int>
      MatchDateTime: DateTime }
