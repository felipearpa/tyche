module PoolLayoutDomainMapperTests

open System
open Pipel.Tyche.PoolLayout
open Pipel.Tyche.PoolLayout.Data
open Pipel.Tyche.PoolLayout.Domain
open Pipel.Type
open Xunit

[<Fact>]
let ``given a PoolLayoutEntity when this one is mapped to PoolLayout then an identical PoolLayout is returned`` () =
    let poolLayoutEntity =
        { PoolLayoutEntity.PoolLayoutId = Guid.NewGuid()
          Name = "Copa América 2021"
          OpeningStartDateTime = DateTime.Now
          OpeningEndDateTime = DateTime.Now }

    let poolLayout =
        PoolLayoutDomainMapper.mapFromDomainToData poolLayoutEntity

    Assert.Equal(poolLayoutEntity.PoolLayoutId, poolLayout.PoolLayoutPK.PoolLayoutId |> Uuid.value)
    Assert.Equal(poolLayoutEntity.Name, poolLayout.Name |> NonEmptyString100.value)
    Assert.Equal(poolLayoutEntity.OpeningStartDateTime, poolLayout.OpeningStartDateTime |> DateTime.value)
    Assert.Equal(poolLayoutEntity.OpeningEndDateTime, poolLayout.OpeningEndDateTime |> DateTime.value)
