namespace Felipearpa.Tyche.Pool.Domain

open System
open Felipearpa.Type

type InitialPoolGamblerBet =
    { PoolId: Ulid
      GamblerId: Ulid
      GamblerUsername: NonEmptyString100
      MatchId: Ulid
      PoolLayoutId: Ulid
      HomeTeamId: string
      HomeTeamName: NonEmptyString100
      AwayTeamId: string
      AwayTeamName: NonEmptyString100
      MatchDateTime: DateTime
      PoolLayoutVersion: int
      Round: string
      HomeTeamScore: int option
      AwayTeamScore: int option
      BetScore: int option
      ComputedDateTime: DateTime option
      ComputedRequestId: string option }
