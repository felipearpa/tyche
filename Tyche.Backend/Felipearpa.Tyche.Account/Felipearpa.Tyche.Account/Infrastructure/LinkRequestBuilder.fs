namespace Felipearpa.Tyche.Account.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Tyche.Account.Domain

module LinkRequestBuilder =
    [<Literal>]
    let private accountTableName = "Account"

    [<Literal>]
    let private userNameText = "EMAIL"

    let private buildAccountMap (accountEntity: AccountEntity) =
        dict
            [ "pk", AttributeValue(accountEntity.Pk)
              "accountId", AttributeValue(accountEntity.AccountId)
              "email", AttributeValue(accountEntity.Email)
              "externalAccountId", AttributeValue(accountEntity.ExternalAccountId) ]

    let private buildUniqueEmailMap (accountEntity: AccountEntity) =
        dict [ "pk", AttributeValue($"{userNameText}#{accountEntity.Email}") ]

    let build (accountEntity: AccountEntity) =
        let accountMap = buildAccountMap accountEntity
        Put(TableName = accountTableName, Item = (accountMap |> Dictionary))

    let buildUniqueEmail (accountEntity: AccountEntity) =
        let uniqueEmailMap = buildUniqueEmailMap accountEntity

        Put(
            TableName = accountTableName,
            Item = (uniqueEmailMap |> Dictionary),
            ConditionExpression = "attribute_not_exists(pk)"
        )
