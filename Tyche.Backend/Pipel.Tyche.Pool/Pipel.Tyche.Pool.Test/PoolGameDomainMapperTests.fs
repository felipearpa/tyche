module PoolGameDomainMapperTests

open System
open Pipel.Tyche.Pool
open Pipel.Tyche.Pool.Data
open Pipel.Tyche.Pool.Domain
open Pipel.Type
open Xunit

[<Fact>]
let ``given a PoolGameEntity when is mapped to PoolGame then an identical PoolGame is returned`` () =
    let poolGameEntity =
        { PoolGameEntity.Pk = "pk"
          Sk = "sk"
          PoolId = Ulid.newUlid () |> Ulid.toString
          GameId = Ulid.newUlid () |> Ulid.toString
          HomeTeamId = Ulid.newUlid () |> Ulid.toString
          HomeTeamName = "Colombia"
          HomeTeamScore = Nullable(1)
          HomeTeamBet = Nullable(2)
          AwayTeamId = Ulid.newUlid () |> Ulid.toString
          AwayTeamName = "Brasil"
          AwayTeamScore = Nullable(3)
          AwayTeamBet = Nullable(2)
          BetScore = Nullable(10)
          MatchDateTime = DateTime.Now
          Filter = "" }

    let poolGame =
        PoolGameDomainMapper.mapFromDomainToData poolGameEntity

    Assert.Equal(poolGameEntity.PoolId, poolGame.PoolGamePK.PoolPK.PoolId |> Ulid.toString)
    Assert.Equal(poolGameEntity.GameId, poolGame.PoolGamePK.GamePK.GameId |> Ulid.toString)
    Assert.Equal(poolGameEntity.HomeTeamId, poolGame.HomeTeamId |> Ulid.toString)
    Assert.Equal(poolGameEntity.HomeTeamName, poolGame.HomeTeamName |> NonEmptyString100.value)

    Assert.Equal(
        poolGameEntity.HomeTeamScore,
        poolGame.HomeTeamScore
        |> PositiveInt.nullableValue
    )

    Assert.Equal(poolGameEntity.AwayTeamId, poolGame.AwayTeamId |> Ulid.toString)
    Assert.Equal(poolGameEntity.AwayTeamName, poolGame.AwayTeamName |> NonEmptyString100.value)

    Assert.Equal(
        poolGameEntity.AwayTeamScore,
        poolGame.AwayTeamScore
        |> PositiveInt.nullableValue
    )

    Assert.Equal(poolGameEntity.BetScore, poolGame.BetScore |> PositiveInt.nullableValue)
    Assert.Equal(poolGameEntity.MatchDateTime, poolGame.MatchDateTime |> DateTime.value)
