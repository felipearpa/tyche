namespace Felipearpa.Tyche.Account.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model

module GetByIdRequestBuilder =
    [<Literal>]
    let private accountTableName = "Account"

    [<Literal>]
    let private accountText = "ACCOUNT"

    let build (id: string) =
        let keyConditionExpression = "#pk = :pk"

        let mutable attributeValues =
            dict [ ":pk", AttributeValue(S = $"{accountText}#{id}") ]

        let mutable attributeNames = dict [ "#pk", "pk" ]

        QueryRequest(
            TableName = accountTableName,
            KeyConditionExpression = keyConditionExpression,
            ExpressionAttributeNames = Dictionary attributeNames,
            ExpressionAttributeValues = Dictionary attributeValues
        )
