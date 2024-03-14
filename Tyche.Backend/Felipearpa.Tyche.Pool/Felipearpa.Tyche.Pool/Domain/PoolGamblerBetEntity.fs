namespace Felipearpa.Tyche.Pool.Data

open System
open Amazon.DynamoDBv2.DataModel

[<DynamoDBTable("Pool")>]
[<CLIMutable>]
type PoolGamblerBetEntity =
    { [<DynamoDBProperty("pk")>]
      Pk: string

      [<DynamoDBProperty("sk")>]
      Sk: string

      [<DynamoDBProperty("poolId")>]
      PoolId: string

      [<DynamoDBProperty("gamblerId")>]
      GamblerId: string

      [<DynamoDBProperty("matchId")>]
      MatchId: string

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

      [<DynamoDBProperty("score")>]
      Score: Nullable<int>

      [<DynamoDBProperty("matchDateTime")>]
      MatchDateTime: DateTime }
