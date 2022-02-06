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
        { PoolLayoutEntity.PoolLayoutId = Ulid.newUlid() |> Ulid.toString
          Name = "Copa América 2021"
          StartOpeningDateTime = DateTime.Now
          EndOpeningDateTime = DateTime.Now
          Pk = ""
          Sk = ""
          Filter = "" }

    let poolLayout =
        PoolLayoutDomainMapper.mapFromDomainToData poolLayoutEntity

    Assert.Equal(poolLayoutEntity.PoolLayoutId, poolLayout.PoolLayoutPK.PoolLayoutId |> Ulid.toString)
    Assert.Equal(poolLayoutEntity.Name, poolLayout.Name |> NonEmptyString100.value)
    Assert.Equal(poolLayoutEntity.StartOpeningDateTime, poolLayout.OpeningStartDateTime |> DateTime.value)
    Assert.Equal(poolLayoutEntity.EndOpeningDateTime, poolLayout.OpeningEndDateTime |> DateTime.value)
