namespace Felipearpa.Tyche.Pool.Domain

open System
open Amazon.DynamoDBv2.DataModel

[<DynamoDBTable("Pool")>]
[<CLIMutable>]
type PoolGamblerScoreEntity =
    { [<DynamoDBProperty("pk")>]
      Pk: string

      [<DynamoDBProperty("sk")>]
      Sk: string

      [<DynamoDBProperty("poolId")>]
      PoolId: string

      [<DynamoDBProperty("poolName")>]
      PoolName: string

      [<DynamoDBProperty("gamblerId")>]
      GamblerId: string

      [<DynamoDBProperty("gamblerUsername")>]
      GamblerUsername: string

      [<DynamoDBProperty("currentPosition")>]
      CurrentPosition: int Nullable

      [<DynamoDBProperty("beforePosition")>]
      BeforePosition: int Nullable

      [<DynamoDBProperty("score")>]
      Score: int Nullable }
