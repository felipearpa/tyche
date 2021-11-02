namespace Pipel.Tyche.PoolLayout.Data

open System
open MongoDB.Bson.Serialization.Attributes

type PoolLayoutEntityPK =
    { PoolLayoutId: Guid }

[<CLIMutable>]
type PoolLayoutEntity =
    { [<BsonId>]
      PoolLayoutId: Guid
      Name: string
      OpeningStartDateTime: DateTime
      OpeningEndDateTime: DateTime }
