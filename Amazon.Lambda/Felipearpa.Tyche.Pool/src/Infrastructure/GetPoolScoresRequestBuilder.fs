namespace Felipearpa.Tyche.Pool.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb
open Felipearpa.Type

module GetPoolScoresRequestBuilder =

    let build (poolId: Ulid) (maybeNext: IDictionary<string, AttributeValue> option) =
        let keyConditionExpression = $"{ExpressionAttribute.name Key.pk} = :pk"

        let filterExpression =
            $"{ExpressionAttribute.name PoolTable.Attribute.status} = :status"

        let attributeValues =
            dict
                [ ":pk", AttributeValue(KeyPrefix.build PoolTable.Prefix.pool poolId.Value)
                  ":status", AttributeValue("OPENED") ]

        let attributeNames =
            ExpressionAttribute.names [ Key.pk; PoolTable.Attribute.status ]

        QueryRequest(
            TableName = PoolTable.name,
            IndexName = PoolTable.Index.scoresByPool,
            ScanIndexForward = false,
            KeyConditionExpression = keyConditionExpression,
            FilterExpression = filterExpression,
            ExpressionAttributeNames = Dictionary attributeNames,
            ExpressionAttributeValues = Dictionary attributeValues,
            ExclusiveStartKey =
                match maybeNext with
                | None -> null
                | Some next -> Dictionary next
        )
