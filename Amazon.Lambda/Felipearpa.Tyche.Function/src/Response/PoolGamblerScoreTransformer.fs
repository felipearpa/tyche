namespace Felipearpa.Tyche.Function.Response

open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

module PoolGamblerScoreTransformer =

    let private toTrendGroupResponse (group: KickoffGroup) =
        { TrendGroupResponse.KickoffDateTime = group.KickoffDateTime.ToUniversalTime()
          ComputedMatches = group.ComputedMatches
          TotalMatches = group.TotalMatches }

    let private toResponse (trendGroup: KickoffGroup option) (poolGamblerScore: PoolGamblerScore) =
        { PoolGamblerScoreResponse.PoolId = poolGamblerScore.PoolId |> Ulid.value
          PoolName = poolGamblerScore.PoolName |> NonEmptyString100.value
          GamblerId = poolGamblerScore.GamblerId |> Ulid.value
          GamblerUsername = poolGamblerScore.GamblerUsername |> NonEmptyString100.value
          Position = poolGamblerScore.Position
          BeforePosition = poolGamblerScore.BeforePosition
          Score = poolGamblerScore.Score
          BeforeScore = poolGamblerScore.BeforeScore
          GamblerCount = poolGamblerScore.GamblerCount
          TrendGroup = trendGroup |> Option.map toTrendGroupResponse }

    let ofStanding (standing: PoolStanding) =
        toResponse standing.TrendGroup standing.Score
