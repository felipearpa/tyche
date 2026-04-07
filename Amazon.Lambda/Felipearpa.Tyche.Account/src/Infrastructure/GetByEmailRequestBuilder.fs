namespace Felipearpa.Tyche.Account.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb

module GetByEmailRequestBuilder =

    let build (email: string) =
        let keyConditionExpression =
            $"{ExpressionAttribute.name AccountTable.Attribute.email} = :{AccountTable.Attribute.email}"

        let mutable attributeValues =
            dict [ $":{AccountTable.Attribute.email}", AttributeValue(email) ]

        let mutable attributeNames =
            ExpressionAttribute.names [ AccountTable.Attribute.email ]

        QueryRequest(
            TableName = AccountTable.name,
            IndexName = AccountTable.Index.getByEmail,
            KeyConditionExpression = keyConditionExpression,
            ExpressionAttributeNames = Dictionary attributeNames,
            ExpressionAttributeValues = Dictionary attributeValues
        )
