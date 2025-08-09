namespace Felipearpa.Tyche.Account.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Tyche.Account.Domain
open Felipearpa.Type

module LinkRequestBuilder =
    [<Literal>]
    let private accountTableName = "Account"

    [<Literal>]
    let private userNameText = "EMAIL"

    [<Literal>]
    let private prefixAccount = "ACCOUNT"

    let private buildAccountMap (account: AccountLink) (newAccountId: Ulid) =
        dict
            [ "pk", AttributeValue(S = $"{prefixAccount}#{newAccountId}")
              "accountId", AttributeValue(S = newAccountId.Value)
              "email", AttributeValue(S = account.Email.Value)
              "externalAccountId", AttributeValue(S = account.ExternalAccountId) ]

    let private buildUniqueEmailMap (accountLink: AccountLink) =
        dict [ "pk", AttributeValue($"{userNameText}#{accountLink.Email}") ]

    let build (accountLink: AccountLink) (newAccountId: Ulid) =
        let accountMap = buildAccountMap accountLink newAccountId
        Put(TableName = accountTableName, Item = (accountMap |> Dictionary))

    let buildUniqueEmail (accountLink: AccountLink) =
        let uniqueEmailMap = buildUniqueEmailMap accountLink

        Put(
            TableName = accountTableName,
            Item = (uniqueEmailMap |> Dictionary),
            ConditionExpression = "attribute_not_exists(pk)"
        )
