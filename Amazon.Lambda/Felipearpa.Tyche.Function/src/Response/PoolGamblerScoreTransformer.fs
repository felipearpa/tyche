namespace Felipearpa.Tyche.Function.Response

open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

module PoolGamblerScoreTransformer =

    let toResponse (poolGamblerScore: PoolGamblerScore) =
        { PoolGamblerScoreResponse.PoolId = poolGamblerScore.PoolId |> Ulid.value
          PoolName = poolGamblerScore.PoolName |> NonEmptyString100.value
          GamblerId = poolGamblerScore.GamblerId |> Ulid.value
          GamblerUsername = poolGamblerScore.GamblerUsername |> NonEmptyString100.value
          CurrentPosition = poolGamblerScore.CurrentPosition
          BeforePosition = poolGamblerScore.BeforePosition
          Score = poolGamblerScore.Score }
