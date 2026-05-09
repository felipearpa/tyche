namespace Felipearpa.Tyche.Pool.Domain

open System
open Felipearpa.Tyche.Pool.Type
open Felipearpa.Type

type PoolGamblerBet =
    { PoolId: Ulid
      GamblerId: Ulid
      GamblerUsername: NonEmptyString100
      MatchId: Ulid
      PoolLayoutId: Ulid
      HomeTeamId: string
      HomeTeamName: NonEmptyString100
      AwayTeamId: string
      AwayTeamName: NonEmptyString100
      MatchScore: TeamScore<int> option
      BetScore: TeamScore<BetScore> option
      Score: int option
      MatchDateTime: DateTime
      Round: string
      GroupName: string option
      isLocked: bool
      isComputed: bool }
