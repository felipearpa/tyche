namespace Felipearpa.Tyche.Pool.Data

open Amazon.DynamoDBv2.DataModel

[<DynamoDBTable("Pool")>]
[<CLIMutable>]
type PoolEntity =
    { [<DynamoDBProperty("pk")>]
      Pk: string

      [<DynamoDBProperty("sk")>]
      Sk: string

      [<DynamoDBProperty("poolId")>]
      PoolId: string

      [<DynamoDBProperty("poolName")>]
      PoolName: string

      [<DynamoDBProperty("poolLayoutId")>]
      PoolLayoutId: string }
