module PoolApplicationMapperTests

open Pipel.Tyche.Pool.Api
open Pipel.Type
open Xunit
open Pipel.Tyche.Pool.Domain

[<Fact>]
let ``given a Pool when this one is mapped to PoolResponse then an identical PoolResponse is returned``
    ()
    =
    let pool =
        { Pool.PoolLayoutPK = { PoolLayoutPK.PoolLayoutId = Ulid.newUlid () }
          PoolPK = { PoolPK.PoolId = Ulid.newUlid () }
          PoolName = NonEmptyString100.From "Copa Ameérica 2022"
          CurrentPosition = None
          BeforePosition = None }

    let poolResponse =
        PoolApplicationMapper.mapFromDomainToApplication pool

    Assert.Equal(pool.PoolPK.PoolId |> Ulid.toString, poolResponse.PoolId)
    Assert.Equal(pool.PoolLayoutPK.PoolLayoutId |> Ulid.toString, poolResponse.PoolLayoutId)
    Assert.Equal(pool.PoolName |> NonEmptyString100.value, poolResponse.PoolName)
    Assert.Equal(pool.CurrentPosition |> PositiveInt.nullableValue, poolResponse.CurrentPosition)
    Assert.Equal(pool.BeforePosition |> PositiveInt.nullableValue, poolResponse.BeforePosition)
