module PoolLayoutApplicationMapperTests

open Pipel.Tyche.PoolLayout.Api
open Pipel.Type
open Xunit
open Pipel.Tyche.PoolLayout.Domain

[<Fact>]
let ``given a PoolLayout when this one is mapped to PoolLayoutResponse then an identical PoolLayoutResponse is returned``
    ()
    =
    let poolLayout =
        { PoolLayout.PoolLayoutPK = { PoolLayoutPK.PoolLayoutId = Uuid.newUuid () }
          Name = "Copa América 2021" |> NonEmptyString100.From
          OpeningStartDateTime = DateTime.now ()
          OpeningEndDateTime = DateTime.now () }

    let poolLayoutResponse =
        PoolLayoutApplicationMapper.mapFromDomainToApplication poolLayout

    Assert.Equal(poolLayout.PoolLayoutPK.PoolLayoutId |> Uuid.value, poolLayoutResponse.PoolLayoutId)
    Assert.Equal(poolLayout.Name |> NonEmptyString100.value, poolLayoutResponse.Name)
    Assert.Equal(poolLayout.OpeningStartDateTime |> DateTime.value, poolLayoutResponse.OpeningStartDateTime)
    Assert.Equal(poolLayout.OpeningEndDateTime |> DateTime.value, poolLayoutResponse.OpeningEndDateTime)
