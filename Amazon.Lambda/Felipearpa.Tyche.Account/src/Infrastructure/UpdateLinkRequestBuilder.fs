namespace Felipearpa.Tyche.Account.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.Account.Domain

module UpdateLinkRequestBuilder =

    let private buildAccountKeyMap (account: Account) =
        dict [ Key.pk, AttributeValue(KeyPrefix.build AccountTable.Prefix.account account.AccountId.Value) ]

    let private buildExternalAccountNames () =
        ExpressionAttribute.names [ AccountTable.Attribute.externalAccountId ]

    let private buildExternalAccountValues (accountLink: AccountLink) =
        dict [ $":{AccountTable.Attribute.externalAccountId}", AttributeValue(accountLink.ExternalAccountId) ]

    let private buildUpdateAccountLinkExpression () =
        $"SET {ExpressionAttribute.name AccountTable.Attribute.externalAccountId} = :{AccountTable.Attribute.externalAccountId}"

    let build (account: Account) (accountLink: AccountLink) =
        let keyMap = buildAccountKeyMap account
        let externalAccountNames = buildExternalAccountNames ()
        let externalAccountValues = buildExternalAccountValues accountLink
        let updateExpression = buildUpdateAccountLinkExpression ()

        UpdateItemRequest(
            TableName = AccountTable.name,
            Key = (keyMap |> Dictionary),
            ExpressionAttributeNames = (externalAccountNames |> Dictionary),
            ExpressionAttributeValues = (externalAccountValues |> Dictionary),
            UpdateExpression = updateExpression
        )
