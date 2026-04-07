namespace Felipearpa.Tyche.Pool.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb
open Felipearpa.Type

module GetPoolGamblerScoreRequestBuilder =

    let build (poolId: Ulid) (gamblerId: Ulid) =
        let keyConditionExpression =
            $"{ExpressionAttribute.name Key.pk} = :pk and {ExpressionAttribute.name Key.sk} = :sk"

        let filterConditionExpression =
            $"{ExpressionAttribute.name PoolTable.Attribute.status} = :status"

        let attributeValues =
            dict
                [ ":pk", AttributeValue(KeyPrefix.build PoolTable.Prefix.pool poolId.Value)
                  ":sk", AttributeValue(KeyPrefix.build PoolTable.Prefix.gambler gamblerId.Value)
                  ":status", AttributeValue("OPENED") ]

        let attributeNames =
            ExpressionAttribute.names [ Key.pk; Key.sk; PoolTable.Attribute.status ]

        QueryRequest(
            TableName = PoolTable.name,
            KeyConditionExpression = keyConditionExpression,
            FilterExpression = filterConditionExpression,
            ExpressionAttributeNames = Dictionary attributeNames,
            ExpressionAttributeValues = Dictionary attributeValues
        )
