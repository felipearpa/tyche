namespace Felipearpa.Tyche.Account.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.Account.Domain
open Felipearpa.Type

module LinkRequestBuilder =

    let private buildAccountMap (account: AccountLink) (newAccountId: Ulid) =
        dict
            [ Key.pk, AttributeValue(S = KeyPrefix.build AccountTable.Prefix.account newAccountId.Value)
              AccountTable.Attribute.accountId, AttributeValue(S = newAccountId.Value)
              AccountTable.Attribute.email, AttributeValue(S = account.Email.Value)
              AccountTable.Attribute.externalAccountId, AttributeValue(S = account.ExternalAccountId) ]

    let private buildUniqueEmailMap (accountLink: AccountLink) =
        dict [ Key.pk, AttributeValue(KeyPrefix.build AccountTable.Prefix.email accountLink.Email.Value) ]

    let build (accountLink: AccountLink) (newAccountId: Ulid) =
        let accountMap = buildAccountMap accountLink newAccountId
        Put(TableName = AccountTable.name, Item = (accountMap |> Dictionary))

    let buildUniqueEmail (accountLink: AccountLink) =
        let uniqueEmailMap = buildUniqueEmailMap accountLink

        Put(
            TableName = AccountTable.name,
            Item = (uniqueEmailMap |> Dictionary),
            ConditionExpression = $"attribute_not_exists({Key.pk})"
        )
