namespace Felipearpa.User.Domain

open Amazon.DynamoDBv2.DataModel

[<DynamoDBTable("User")>]
[<CLIMutable>]
type UserEntity =
    { [<DynamoDBProperty("pk")>]
      Pk: string

      [<DynamoDBProperty("userId")>]
      UserId: string

      [<DynamoDBProperty("username")>]
      Username: string

      [<DynamoDBProperty("hash")>]
      Hash: string }
