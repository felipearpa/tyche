namespace Felipearpa.Account.Domain

open Amazon.DynamoDBv2.DataModel

[<CLIMutable>]
type AccountEntity =
    { [<DynamoDBProperty("pk")>]
      Pk: string

      [<DynamoDBProperty("accountId")>]
      AccountId: string

      [<DynamoDBProperty("email")>]
      Email: string

      [<DynamoDBProperty("externalAccountId")>]
      ExternalAccountId: string }
