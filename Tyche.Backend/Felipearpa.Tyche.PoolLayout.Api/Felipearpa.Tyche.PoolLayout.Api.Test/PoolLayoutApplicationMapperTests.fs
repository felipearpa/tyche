module PoolLayoutApplicationMapperTests

open System
open Felipearpa.Tyche.PoolLayout.Api
open Felipearpa.Type
open Xunit
open Felipearpa.Tyche.PoolLayout.Domain

[<Fact>]
let ``given a PoolLayout when this one is mapped to PoolLayoutResponse then an identical PoolLayoutResponse is returned``
    ()
    =
    let poolLayout =
        { PoolLayout.PoolLayoutPK = { PoolLayoutPK.PoolLayoutId = Ulid.random () }
          Name = "Copa América 2021" |> NonEmptyString100.newOf
          OpeningStartDateTime = DateTime.Now
          OpeningEndDateTime = DateTime.Now }

    let poolLayoutResponse =
        PoolLayoutApplicationMapper.mapFromDomainToApplication poolLayout

    Assert.Equal(
        poolLayout.PoolLayoutPK.PoolLayoutId
        |> Ulid.toString,
        poolLayoutResponse.PoolLayoutId
    )

    Assert.Equal(poolLayout.Name |> NonEmptyString100.value, poolLayoutResponse.Name)
    Assert.Equal(poolLayout.OpeningStartDateTime, poolLayoutResponse.StartOpeningDateTime)
    Assert.Equal(poolLayout.OpeningEndDateTime, poolLayoutResponse.EndOpeningDateTime)
