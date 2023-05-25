module PoolLayoutDomainMapperTests

open System
open Felipearpa.Tyche.PoolLayout
open Felipearpa.Tyche.PoolLayout.Data
open Felipearpa.Tyche.PoolLayout.Domain
open Felipearpa.Type
open Xunit

[<Fact>]
let ``given a PoolLayoutEntity when this one is mapped to PoolLayout then an identical PoolLayout is returned`` () =
    let poolLayoutEntity =
        { PoolLayoutEntity.PoolLayoutId = Ulid.random () |> Ulid.value
          Name = "Copa América 2021"
          StartOpeningDateTime = DateTime.Now
          EndOpeningDateTime = DateTime.Now
          Pk = ""
          Sk = ""
          Filter = "" }

    let poolLayout =
        PoolLayoutDomainMapper.mapFromDomainToData poolLayoutEntity

    Assert.Equal(poolLayoutEntity.PoolLayoutId, poolLayout.PoolLayoutPK.PoolLayoutId |> Ulid.value)
    Assert.Equal(poolLayoutEntity.Name, poolLayout.Name |> NonEmptyString100.value)
    Assert.Equal(poolLayoutEntity.StartOpeningDateTime, poolLayout.OpeningStartDateTime)
    Assert.Equal(poolLayoutEntity.EndOpeningDateTime, poolLayout.OpeningEndDateTime)
