namespace Felipearpa.Tyche.Pool.Domain

open Felipearpa.Type

module PoolGamblerScoreMapper =

    let mapToDomain (poolGamblerScoreEntity: PoolGamblerScoreEntity) =
        { PoolGamblerScore.PoolId = poolGamblerScoreEntity.PoolId |> Ulid.newOf
          PoolName = poolGamblerScoreEntity.PoolName |> NonEmptyString100.newOf
          GamblerId = poolGamblerScoreEntity.GamblerId |> Ulid.newOf
          GamblerUsername = poolGamblerScoreEntity.GamblerUsername |> NonEmptyString100.newOf
          CurrentPosition = poolGamblerScoreEntity.CurrentPosition |> Option.ofNullable
          BeforePosition = poolGamblerScoreEntity.BeforePosition |> Option.ofNullable
          Score = poolGamblerScoreEntity.Score |> Option.ofNullable }
