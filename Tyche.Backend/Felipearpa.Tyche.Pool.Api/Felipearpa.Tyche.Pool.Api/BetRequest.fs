namespace Felipearpa.Tyche.Pool.Api

type BetRequest =
    { PoolId: string
      GamblerId: string
      MatchId: string
      HomeTeamBet: int
      AwayTeamBet: int }
