namespace Felipearpa.Tyche.PoolLayout.Domain

open System
open Felipearpa.Type

type PoolLayoutMatch =
    { MatchId: Ulid
      PoolLayoutId: Ulid
      HomeTeamId: Ulid
      HomeTeamName: NonEmptyString100
      AwayTeamId: Ulid
      AwayTeamName: NonEmptyString100
      MatchDateTime: DateTime
      PoolLayoutVersion: int }
