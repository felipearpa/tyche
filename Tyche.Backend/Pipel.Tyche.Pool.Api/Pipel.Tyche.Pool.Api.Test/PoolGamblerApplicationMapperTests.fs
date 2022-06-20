module PoolGamblerApplicationMapperTests

open Pipel.Tyche.Pool.Api
open Pipel.Type
open Xunit
open Pipel.Tyche.Pool.Domain

[<Fact>]
let ``given a PoolGambler when is mapped to PoolGamblerResponse then an identical PoolGamblerResponse is returned`` () =
    let poolGambler =
        { PoolGambler.PoolGamblerPK =
            { PoolGamblerPK.PoolPK = { PoolPK.PoolId = Ulid.newUlid () }
              GamblerPK = { GamblerPK.GamblerId = Ulid.newUlid () } }
          GamblerEmail = Email.From "email@email.com"
          Score = PositiveInt.TryFrom 10
          CurrentPosition = PositiveInt.TryFrom 1
          BeforePosition = PositiveInt.TryFrom 2 }

    let poolGamblerResponse =
        PoolGamblerApplicationMapper.mapFromDomainToApplication poolGambler

    Assert.Equal(
        poolGambler.PoolGamblerPK.PoolPK.PoolId
        |> Ulid.toString,
        poolGamblerResponse.PoolId
    )

    Assert.Equal(
        poolGambler.PoolGamblerPK.GamblerPK.GamblerId
        |> Ulid.toString,
        poolGamblerResponse.GamblerId
    )

    Assert.Equal(poolGambler.GamblerEmail |> Email.value, poolGamblerResponse.GamblerEmail)
    Assert.Equal(poolGambler.Score |> PositiveInt.nullableValue, poolGamblerResponse.Score)

    Assert.Equal(
        poolGambler.CurrentPosition
        |> PositiveInt.nullableValue,
        poolGamblerResponse.CurrentPosition
    )

    Assert.Equal(
        poolGambler.BeforePosition
        |> PositiveInt.nullableValue,
        poolGamblerResponse.BeforePosition
    )
