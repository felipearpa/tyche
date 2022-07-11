namespace Pipel.Tyche.Pool.Data

open System
open Amazon.DynamoDBv2.DataModel

type PoolGamblerEntityPK =
    { PoolId: string
      GamblerId: string }

[<DynamoDBTable("Pool")>]
[<CLIMutable>]
type PoolGamblerEntity =
    { [<DynamoDBProperty("pk")>]
      Pk: string

      [<DynamoDBProperty("sk")>]
      Sk: string

      [<DynamoDBProperty("poolId")>]
      PoolId: string

      [<DynamoDBProperty("gamblerId")>]
      GamblerId: string

      [<DynamoDBProperty("gamblerEmail")>]
      GamblerEmail: string

      [<DynamoDBProperty("score")>]
      Score: int Nullable

      [<DynamoDBProperty("currentPosition")>]
      CurrentPosition: int Nullable

      [<DynamoDBProperty("beforePosition")>]
      BeforePosition: int Nullable

      [<DynamoDBProperty("filter")>]
      Filter: string }
