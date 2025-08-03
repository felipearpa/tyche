namespace Felipearpa.Tyche.Account.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Tyche.Account.Domain

module UpdateLinkRequestBuilder =
    [<Literal>]
    let private accountTableName = "Account"

    let private buildAccountKeyMap (accountEntity: AccountEntity) =
        dict [ "pk", AttributeValue(accountEntity.Pk) ]

    let private buildExternalAccountNames () =
        dict [ "#externalAccountId", "externalAccountId" ]

    let private buildExternalAccountValues (accountLink: AccountLink) =
        dict [ ":externalAccountId", AttributeValue(accountLink.ExternalAccountId) ]

    let private buildUpdateAccountLinkExpression () =
        "SET #externalAccountId = :externalAccountId"

    let build (accountEntity: AccountEntity) (accountLink: AccountLink) =
        let keyMap = buildAccountKeyMap accountEntity
        let externalAccountNames = buildExternalAccountNames ()
        let externalAccountValues = buildExternalAccountValues accountLink
        let updateExpression = buildUpdateAccountLinkExpression ()

        UpdateItemRequest(
            TableName = accountTableName,
            Key = (keyMap |> Dictionary),
            ExpressionAttributeNames = (externalAccountNames |> Dictionary),
            ExpressionAttributeValues = (externalAccountValues |> Dictionary),
            UpdateExpression = updateExpression
        )
