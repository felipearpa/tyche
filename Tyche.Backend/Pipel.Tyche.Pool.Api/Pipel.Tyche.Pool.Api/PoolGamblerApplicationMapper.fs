namespace Pipel.Tyche.Pool.Api

open Pipel.Tyche.Pool.Domain
open Pipel.Type

module PoolGamblerApplicationMapper =

    let mapFromDomainToApplication (domainModel: PoolGambler) =
        { PoolGamblerResponse.PoolId =
            domainModel.PoolGamblerPK.PoolPK.PoolId
            |> Ulid.toString
          GamblerId =
            domainModel.PoolGamblerPK.GamblerPK.GamblerId
            |> Ulid.toString
          GamblerEmail = domainModel.GamblerEmail |> Email.value
          Score = domainModel.Score |> PositiveInt.nullableValue
          CurrentPosition =
            domainModel.CurrentPosition
            |> PositiveInt.nullableValue
          BeforePosition =
            domainModel.BeforePosition
            |> PositiveInt.nullableValue }
