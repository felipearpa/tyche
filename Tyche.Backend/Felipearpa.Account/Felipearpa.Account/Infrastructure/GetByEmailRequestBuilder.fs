namespace Felipearpa.Account.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model

module GetByEmailRequestBuilder =
    [<Literal>]
    let private accountTableName = "Account"

    [<Literal>]
    let private emailIndex = "email-index"

    let build (email: string) =
        let keyConditionExpression = "#email = :email"

        let mutable attributeValues = dict [ ":email", AttributeValue(email) ]

        let mutable attributeNames = dict [ "#email", "email" ]

        QueryRequest(
            TableName = accountTableName,
            IndexName = emailIndex,
            KeyConditionExpression = keyConditionExpression,
            ExpressionAttributeNames = Dictionary attributeNames,
            ExpressionAttributeValues = Dictionary attributeValues
        )
