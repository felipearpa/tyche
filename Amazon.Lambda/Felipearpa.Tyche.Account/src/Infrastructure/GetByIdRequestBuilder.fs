namespace Felipearpa.Tyche.Account.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb

module GetByIdRequestBuilder =

    let build (id: string) =
        let keyConditionExpression = $"{ExpressionAttribute.name Key.pk} = :pk"

        let mutable attributeValues =
            dict [ ":pk", AttributeValue(S = KeyPrefix.build AccountTable.Prefix.account id) ]

        let mutable attributeNames = ExpressionAttribute.names [ Key.pk ]

        QueryRequest(
            TableName = AccountTable.name,
            KeyConditionExpression = keyConditionExpression,
            ExpressionAttributeNames = Dictionary attributeNames,
            ExpressionAttributeValues = Dictionary attributeValues
        )
