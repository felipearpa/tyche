module PoolDomainMapperTests

open System
open Pipel.Tyche.Pool
open Pipel.Tyche.Pool.Data
open Pipel.Tyche.Pool.Domain
open Pipel.Type
open Xunit

[<Fact>]
let ``given a PoolEntity when this one is mapped to Pool then an identical Pool is returned`` () =
    let poolEntity =
        { PoolEntity.PoolLayoutId = Ulid.newUlid () |> Ulid.toString
          Pk = ""
          Sk = ""
          PoolId = Ulid.newUlid () |> Ulid.toString
          PoolName = "Copa América 2021 Pipel"
          CurrentPosition = Nullable(1)
          BeforePosition = Nullable(1)
          Filter = "" }

    let pool =
        PoolDomainMapper.mapFromDomainToData poolEntity

    Assert.Equal(poolEntity.PoolLayoutId, pool.PoolLayoutPK.PoolLayoutId |> Ulid.toString)

    Assert.Equal(poolEntity.PoolName, pool.PoolName |> NonEmptyString100.value)
    Assert.Equal(poolEntity.CurrentPosition, pool.CurrentPosition |> PositiveInt.nullableValue)
    Assert.Equal(poolEntity.BeforePosition, pool.BeforePosition |> PositiveInt.nullableValue)
