namespace Felipearpa.Tyche.Pool.Domain

open System
open Felipearpa.Tyche.Pool.Type
open Felipearpa.Type

type PoolGamblerBet =
    { PoolId: Ulid
      GamblerId: Ulid
      MatchId: Ulid
      HomeTeamId: Ulid
      HomeTeamName: NonEmptyString100
      AwayTeamId: Ulid
      AwayTeamName: NonEmptyString100
      MatchScore: TeamScore<int> option
      BetScore: TeamScore<BetScore> option
      Score: int option
      MatchDateTime: DateTime }
