namespace Felipearpa.Tyche.Pool.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb
open Felipearpa.Type

module GetPoolGamblerBetKeysRequestBuilder =

    let build (poolId: Ulid) (gamblerId: Ulid) (maybeNext: IDictionary<string, AttributeValue> option) =
        let keyConditionExpression = $"{ExpressionAttribute.name Key.pk} = :pk"

        let pk =
            $"{KeyPrefix.build PoolTable.Prefix.gambler gamblerId.Value}#{KeyPrefix.build PoolTable.Prefix.pool poolId.Value}"

        let attributeValues = dict [ ":pk", AttributeValue(S = pk) ]
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
