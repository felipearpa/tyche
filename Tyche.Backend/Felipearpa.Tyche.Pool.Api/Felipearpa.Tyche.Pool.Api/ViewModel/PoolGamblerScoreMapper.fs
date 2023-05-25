namespace Felipearpa.Tyche.Pool.Api.ViewModel

open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

module PoolGamblerScoreMapper =

    let mapToViewModel (poolGamblerScore: PoolGamblerScore) =
        { PoolGamblerScoreViewModel.PoolId = poolGamblerScore.PoolId |> Ulid.value
          PoolLayoutId = poolGamblerScore.PoolLayoutId |> Ulid.value
          PoolName = poolGamblerScore.PoolName |> NonEmptyString100.value
          GamblerId = poolGamblerScore.GamblerId |> Ulid.value
          GamblerUsername = poolGamblerScore.GamblerUsername |> NonEmptyString100.value
          CurrentPosition = poolGamblerScore.CurrentPosition |> Option.toNullable
          BeforePosition = poolGamblerScore.BeforePosition |> Option.toNullable
          Score = poolGamblerScore.Score |> Option.toNullable }
