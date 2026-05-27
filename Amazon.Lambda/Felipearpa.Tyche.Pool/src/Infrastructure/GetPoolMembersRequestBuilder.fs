namespace Felipearpa.Tyche.Pool.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb
open Felipearpa.Type

module GetPoolMembersRequestBuilder =

    let build (poolId: Ulid) (maybeNext: IDictionary<string, AttributeValue> option) =
        let keyConditionExpression = $"{ExpressionAttribute.name Key.pk} = :pk"

        let attributeValues =
            dict [ ":pk", AttributeValue(S = KeyPrefix.build PoolTable.Prefix.pool poolId.Value) ]

        let attributeNames = ExpressionAttribute.names [ Key.pk ]

        QueryRequest(
            TableName = PoolTable.name,
            IndexName = PoolTable.Index.gamblersByUsername,
            ScanIndexForward = true,
            KeyConditionExpression = keyConditionExpression,
            ExpressionAttributeNames = Dictionary attributeNames,
            ExpressionAttributeValues = Dictionary attributeValues,
            ExclusiveStartKey =
                match maybeNext with
                | None -> null
                | Some next -> Dictionary next
        )
