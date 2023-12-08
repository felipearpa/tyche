namespace Felipearpa.User.Domain

open Amazon.DynamoDBv2.DataModel

[<CLIMutable>]
type UserEntity =
    { [<DynamoDBProperty("pk")>]
      Pk: string

      [<DynamoDBProperty("accountId")>]
      AccountId: string

      [<DynamoDBProperty("email")>]
      Email: string }
