namespace Felipearpa.Tyche.PoolLayout.Domain

open System
open Felipearpa.Type

type PoolLayoutMatch =
    { MatchId: Ulid
      PoolLayoutId: Ulid
      HomeTeamId: string
      HomeTeamName: NonEmptyString100
      AwayTeamId: string
      AwayTeamName: NonEmptyString100
      MatchDateTime: DateTime
      PoolLayoutVersion: int
      Round: string
      HomeTeamScore: int option
      AwayTeamScore: int option }
