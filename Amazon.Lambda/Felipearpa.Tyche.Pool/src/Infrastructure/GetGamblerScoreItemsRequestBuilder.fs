namespace Felipearpa.Tyche.Pool.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb
open Felipearpa.Type

module GetGamblerScoreItemsRequestBuilder =

    let build (gamblerId: Ulid) (maybeNext: IDictionary<string, AttributeValue> option) =
        let keyConditionExpression = $"{ExpressionAttribute.name Key.sk} = :sk"

        let attributeValues =
            dict [ ":sk", AttributeValue(KeyPrefix.build PoolTable.Prefix.gambler gamblerId.Value) ]

        let attributeNames =
            ExpressionAttribute.names [ Key.pk; Key.sk; PoolTable.Attribute.poolName ]

        let projectionExpression =
            $"{ExpressionAttribute.name Key.pk}, {ExpressionAttribute.name Key.sk}, {ExpressionAttribute.name PoolTable.Attribute.poolName}"

        QueryRequest(
            TableName = PoolTable.name,
            IndexName = PoolTable.Index.scoresByGambler,
            KeyConditionExpression = keyConditionExpression,
            ProjectionExpression = projectionExpression,
            ExpressionAttributeNames = Dictionary attributeNames,
            ExpressionAttributeValues = Dictionary attributeValues,
            ExclusiveStartKey =
                match maybeNext with
                | None -> null
                | Some next -> Dictionary next
        )
