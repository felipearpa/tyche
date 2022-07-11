namespace Pipel.Tyche.Pool.Api

open Pipel.Tyche.Pool.Domain
open Pipel.Type

module PoolGameApplicationMapper =

    let mapFromDomainToApplication (domainModel: PoolGame) =
        { PoolGameResponse.PoolId = ""
          GameId =
            domainModel.PoolGamePK.GamePK.GameId
            |> Ulid.toString
          HomeTeamId = domainModel.HomeTeamPK.TeamId |> Ulid.toString
          HomeTeamName =
            domainModel.HomeTeamName
            |> NonEmptyString100.value
          HomeTeamScore =
            domainModel.HomeTeamScore
            |> PositiveInt.nullableValue
          HomeTeamBet =
            domainModel.HomeTeamBet
            |> PositiveInt.nullableValue
          AwayTeamId = domainModel.AwayTeamPK.TeamId |> Ulid.toString
          AwayTeamName =
            domainModel.AwayTeamName
            |> NonEmptyString100.value
          AwayTeamScore =
            domainModel.AwayTeamScore
            |> PositiveInt.nullableValue
          AwayTeamBet =
            domainModel.AwayTeamBet
            |> PositiveInt.nullableValue
          BetScore = domainModel.BetScore |> PositiveInt.nullableValue
          MatchDateTime = domainModel.MatchDateTime |> DateTime.value }
