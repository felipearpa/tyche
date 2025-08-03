namespace Felipearpa.Tyche.Function.Response

open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Type
open Felipearpa.Type

module PoolGamblerBetTransformer =

    let toResponse (poolGamblerBet: PoolGamblerBet) =
        { PoolGamblerBetResponse.PoolId = poolGamblerBet.PoolId |> Ulid.value
          GamblerId = poolGamblerBet.GamblerId |> Ulid.value
          MatchId = poolGamblerBet.MatchId |> Ulid.value
          HomeTeamId = poolGamblerBet.HomeTeamId |> Ulid.value
          HomeTeamName = poolGamblerBet.HomeTeamName |> NonEmptyString100.value
          HomeTeamScore =
            match poolGamblerBet.MatchScore with
            | Some score -> score.HomeTeamValue |> Some
            | None -> None
          HomeTeamBet =
            match poolGamblerBet.BetScore with
            | Some score -> score.HomeTeamValue |> BetScore.value |> Some
            | None -> None
          AwayTeamId = poolGamblerBet.AwayTeamId.ToString()
          AwayTeamName = poolGamblerBet.AwayTeamName |> NonEmptyString100.value
          AwayTeamScore =
            match poolGamblerBet.MatchScore with
            | Some score -> score.AwayTeamValue |> Some
            | None -> None
          AwayTeamBet =
            match poolGamblerBet.BetScore with
            | Some score -> score.AwayTeamValue |> BetScore.value |> Some
            | None -> None
          Score = poolGamblerBet.Score
          MatchDateTime = poolGamblerBet.MatchDateTime.ToUniversalTime()
          isLocked = poolGamblerBet.isLocked }
