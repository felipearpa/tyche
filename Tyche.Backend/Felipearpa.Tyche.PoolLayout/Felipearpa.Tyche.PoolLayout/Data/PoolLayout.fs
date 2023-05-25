namespace Felipearpa.Tyche.PoolLayout.Data

open System
open Amazon.DynamoDBv2.DataModel

type PoolLayoutEntityPK =
    { PoolLayoutId: Guid }

[<DynamoDBTable("Pool")>]
[<CLIMutable>]
type PoolLayoutEntity =
    { [<DynamoDBProperty("pk")>]
      Pk: string
      [<DynamoDBProperty("sk")>]
      Sk: string
      [<DynamoDBProperty("poolLayoutId")>]
      PoolLayoutId: string
      [<DynamoDBProperty("name")>]
      Name: string
      [<DynamoDBProperty("startOpeningDateTime")>]
      StartOpeningDateTime: DateTime
      [<DynamoDBProperty("endOpeningDateTime")>]
      EndOpeningDateTime: DateTime
      [<DynamoDBProperty("filter")>]
      Filter: string }
