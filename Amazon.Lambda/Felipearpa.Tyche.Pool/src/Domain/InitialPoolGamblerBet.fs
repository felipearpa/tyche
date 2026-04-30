namespace Felipearpa.Tyche.Pool.Domain

open System
open Felipearpa.Type

type InitialPoolGamblerBet =
    { PoolId: Ulid
      GamblerId: Ulid
      GamblerUsername: NonEmptyString100
      MatchId: Ulid
      PoolLayoutId: Ulid
      HomeTeamId: Ulid
      HomeTeamName: NonEmptyString100
      AwayTeamId: Ulid
      AwayTeamName: NonEmptyString100
      MatchDateTime: DateTime
      PoolLayoutVersion: int }
