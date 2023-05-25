namespace Felipearpa.Tyche.Pool.Domain

open Felipearpa.Tyche.Pool.Type
open Felipearpa.Tyche.Pool.Data
open Felipearpa.Type

module PoolGamblerBetMapper =

    let mapToDomain (poolGamblerBetEntity: PoolGamblerBetEntity) =
        { PoolGamblerBet.PoolId = poolGamblerBetEntity.PoolId |> Ulid.newOf
          GamblerId = poolGamblerBetEntity.GamblerId |> Ulid.newOf
          MatchId = poolGamblerBetEntity.MatchId |> Ulid.newOf
          HomeTeamId = poolGamblerBetEntity.HomeTeamId |> Ulid.newOf
          HomeTeamName = poolGamblerBetEntity.HomeTeamName |> NonEmptyString100.newOf
          AwayTeamId = poolGamblerBetEntity.AwayTeamId |> Ulid.newOf
          AwayTeamName = poolGamblerBetEntity.AwayTeamName |> NonEmptyString100.newOf
          MatchScore =
            if
                poolGamblerBetEntity.HomeTeamScore.HasValue
                && poolGamblerBetEntity.AwayTeamScore.HasValue
            then
                { TeamScore.HomeTeamValue = poolGamblerBetEntity.HomeTeamScore.Value
                  AwayTeamValue = poolGamblerBetEntity.AwayTeamScore.Value }
                |> Some
            else
                None
          BetScore =
            if
                poolGamblerBetEntity.HomeTeamBet.HasValue
                && poolGamblerBetEntity.AwayTeamBet.HasValue
            then
                { TeamScore.HomeTeamValue = poolGamblerBetEntity.HomeTeamBet.Value |> BetScore.newOf
                  AwayTeamValue = poolGamblerBetEntity.AwayTeamBet.Value |> BetScore.newOf }
                |> Some
            else
                None
          Score = poolGamblerBetEntity.BetScore |> Option.ofNullable
          MatchDateTime = poolGamblerBetEntity.MatchDateTime }
