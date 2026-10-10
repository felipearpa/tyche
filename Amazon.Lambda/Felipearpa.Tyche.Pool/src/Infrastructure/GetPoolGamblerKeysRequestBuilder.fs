namespace Felipearpa.Tyche.Pool.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb
open Felipearpa.Type

module GetPoolGamblerKeysRequestBuilder =

    /// Keys of the pool partition's items whose sort key starts with the prefix.
    let buildForPrefix (prefix: string) (poolId: Ulid) (maybeNext: IDictionary<string, AttributeValue> option) =
        let keyConditionExpression =
            $"{ExpressionAttribute.name Key.pk} = :pk AND \
             begins_with({ExpressionAttribute.name Key.sk}, :skPrefix)"

        let attributeValues =
            dict
                [ ":pk", AttributeValue(S = KeyPrefix.build PoolTable.Prefix.pool poolId.Value)
                  ":skPrefix", AttributeValue(S = $"{prefix}#") ]

        let attributeNames = ExpressionAttribute.names [ Key.pk; Key.sk ]

        let projectionExpression =
            $"{ExpressionAttribute.name Key.pk}, {ExpressionAttribute.name Key.sk}"

        QueryRequest(
            TableName = PoolTable.name,
            KeyConditionExpression = keyConditionExpression,
            ProjectionExpression = projectionExpression,
            ExpressionAttributeNames = Dictionary attributeNames,
            ExpressionAttributeValues = Dictionary attributeValues,
            ExclusiveStartKey =
                match maybeNext with
                | None -> null
                | Some next -> Dictionary next
        )

    let build (poolId: Ulid) (maybeNext: IDictionary<string, AttributeValue> option) =
        buildForPrefix PoolTable.Prefix.gambler poolId maybeNext
