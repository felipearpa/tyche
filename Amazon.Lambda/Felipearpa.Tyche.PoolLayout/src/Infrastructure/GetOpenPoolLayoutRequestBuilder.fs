namespace Felipearpa.Tyche.PoolLayout.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb

module GetOpenPoolLayoutRequestBuilder =

    let build (maybeNext: IDictionary<string, AttributeValue> option) =
        let keyConditionExpression =
            $"{ExpressionAttribute.name PoolLayoutTable.Attribute.status} = :status"

        let attributeValues = dict [ ":status", AttributeValue("OPENED") ]

        let attributeNames = ExpressionAttribute.names [ PoolLayoutTable.Attribute.status ]

        QueryRequest(
            TableName = PoolLayoutTable.name,
            IndexName = PoolLayoutTable.Index.getOpenedPoolLayout,
            KeyConditionExpression = keyConditionExpression,
            ExpressionAttributeNames = Dictionary attributeNames,
            ExpressionAttributeValues = Dictionary attributeValues,
            ExclusiveStartKey =
                match maybeNext with
                | None -> null
                | Some next -> Dictionary next
        )
