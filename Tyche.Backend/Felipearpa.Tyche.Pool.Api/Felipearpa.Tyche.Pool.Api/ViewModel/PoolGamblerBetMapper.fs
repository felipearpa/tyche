namespace Felipearpa.Tyche.Pool.Api.ViewModel

open System
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Type
open Felipearpa.Type

module PoolGamblerBetMapper =

    let mapToViewModel (poolGamblerBet: PoolGamblerBet) =
        { PoolGamblerBetViewModel.PoolId = poolGamblerBet.PoolId |> Ulid.value
          GamblerId = poolGamblerBet.GamblerId |> Ulid.value
          MatchId = poolGamblerBet.MatchId |> Ulid.value
          HomeTeamId = poolGamblerBet.HomeTeamId |> Ulid.value
          HomeTeamName = poolGamblerBet.HomeTeamName |> NonEmptyString100.value
          HomeTeamScore =
            match poolGamblerBet.MatchScore with
            | Some score -> score.HomeTeamValue |> Nullable
            | None -> Nullable()
          HomeTeamBet =
            match poolGamblerBet.BetScore with
            | Some score -> score.HomeTeamValue |> BetScore.value |> Nullable
            | None -> Nullable()
          AwayTeamId = poolGamblerBet.AwayTeamId.ToString()
          AwayTeamName = poolGamblerBet.AwayTeamName |> NonEmptyString100.value
          AwayTeamScore =
            match poolGamblerBet.MatchScore with
            | Some score -> score.AwayTeamValue |> Nullable
            | None -> Nullable()
          AwayTeamBet =
            match poolGamblerBet.BetScore with
            | Some score -> score.AwayTeamValue |> BetScore.value |> Nullable
            | None -> Nullable()
          Score = poolGamblerBet.Score |> Option.toNullable
          MatchDateTime = poolGamblerBet.MatchDateTime.ToUniversalTime() }
