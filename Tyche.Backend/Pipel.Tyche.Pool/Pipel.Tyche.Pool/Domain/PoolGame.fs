namespace Pipel.Tyche.Pool.Domain

open Pipel.Tyche.Pool.Domain
open Pipel.Type

type PoolGamePK = { PoolPK: PoolPK; GamePK: GamePK }

type PoolGame =
    { PoolGamePK: PoolGamePK
      HomeTeamPK: TeamPK
      HomeTeamName: NonEmptyString100
      HomeTeamScore: PositiveInt option
      HomeTeamBet: PositiveInt option
      AwayTeamPK: TeamPK
      AwayTeamName: NonEmptyString100
      AwayTeamScore: PositiveInt option
      AwayTeamBet: PositiveInt option
      BetScore: PositiveInt option
      MatchDateTime: DateTime }
