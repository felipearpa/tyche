namespace Pipel.Tyche.Pool.Data

open System
open Amazon.DynamoDBv2.DataModel

type PoolGameEntityPK =
    { PoolId: string
      GameId: string }

[<DynamoDBTable("Pool")>]
[<CLIMutable>]
type PoolGameEntity =
    { [<DynamoDBProperty("pk")>]
      Pk: string

      [<DynamoDBProperty("sk")>]
      Sk: string

      [<DynamoDBProperty("poolId")>]
      PoolId: string

      [<DynamoDBProperty("gameId")>]
      GameId: string

      [<DynamoDBProperty("homeTeamId")>]
      HomeTeamId: string

      [<DynamoDBProperty("homeTeamName")>]
      HomeTeamName: string

      [<DynamoDBProperty("homeTeamScore")>]
      HomeTeamScore: Nullable<int>

      [<DynamoDBProperty("homeTeamBet")>]
      HomeTeamBet: Nullable<int>

      [<DynamoDBProperty("awayTeamId")>]
      AwayTeamId: string

      [<DynamoDBProperty("awayTeamName")>]
      AwayTeamName: string

      [<DynamoDBProperty("awayTeamScore")>]
      AwayTeamScore: Nullable<int>

      [<DynamoDBProperty("awayTeamBet")>]
      AwayTeamBet: Nullable<int>

      [<DynamoDBProperty("betScore")>]
      BetScore: Nullable<int>

      [<DynamoDBProperty("matchDateTime")>]
      MatchDateTime: DateTime

      [<DynamoDBProperty("filter")>]
      Filter: string }
