module PoolGamblerDomainMapperTests

open System
open Pipel.Tyche.Pool
open Pipel.Tyche.Pool.Data
open Pipel.Tyche.Pool.Domain
open Pipel.Type
open Xunit

[<Fact>]
let ``given a PoolGamblerEntity when this one is mapped to PoolGambler then an identical PoolGambler is returned`` () =
    let poolGamblerEntity =
        { PoolGamblerEntity.Pk = ""
          Sk = ""
          PoolId = Ulid.newUlid () |> Ulid.toString
          GamblerId = Ulid.newUlid () |> Ulid.toString
          GamblerEmail = "email@email.com"
          Score = Nullable(10)
          CurrentPosition = Nullable(1)
          BeforePosition = Nullable(1)
          Filter = "" }

    let poolGambler =
        PoolGamblerDomainMapper.mapFromDomainToData poolGamblerEntity

    Assert.Equal(
        poolGamblerEntity.PoolId,
        poolGambler.PoolGamblerPK.PoolPK.PoolId
        |> Ulid.toString
    )
