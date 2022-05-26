namespace Pipel.Tyche.Pool.Data

open System
open Amazon.DynamoDBv2.DataModel

type PoolEntityPK = { PoolId: string }

[<DynamoDBTable("Pool")>]
[<CLIMutable>]
type PoolEntity =
    { [<DynamoDBProperty("pk")>]
      Pk: string
      [<DynamoDBProperty("sk")>]
      Sk: string
      [<DynamoDBProperty("poolId")>]
      PoolId: string
      [<DynamoDBProperty("poolLayoutId")>]
      PoolLayoutId: string
      [<DynamoDBProperty("poolName")>]
      PoolName: string
      [<DynamoDBProperty("currentPosition")>]
      CurrentPosition: int Nullable
      [<DynamoDBProperty("beforePosition")>]
      BeforePosition: int Nullable
      [<DynamoDBProperty("filter")>]
      Filter: string }
