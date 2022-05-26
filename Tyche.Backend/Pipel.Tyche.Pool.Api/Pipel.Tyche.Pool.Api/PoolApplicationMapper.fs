namespace Pipel.Tyche.Pool.Api

open Pipel.Tyche.Pool.Domain
open Pipel.Type

module PoolApplicationMapper =

    let mapFromDomainToApplication (domainModel: Pool) =
        { PoolResponse.PoolLayoutId =
              domainModel.PoolLayoutPK.PoolLayoutId
              |> Ulid.toString
          PoolId = domainModel.PoolPK.PoolId |> Ulid.toString
          PoolName = domainModel.PoolName |> NonEmptyString100.value
          CurrentPosition =
              domainModel.CurrentPosition
              |> PositiveInt.nullableValue
          BeforePosition =
              domainModel.BeforePosition
              |> PositiveInt.nullableValue }
