namespace Felipearpa.Tyche.Pool.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb
open Felipearpa.Type

module ComputePoolGamblerScoreRequestBuilder =

    let build (poolId: Ulid) (gamblerId: Ulid) (delta: int) =
        let key =
            dict
                [ Key.pk, AttributeValue(S = KeyPrefix.build PoolTable.Prefix.pool poolId.Value)
                  Key.sk, AttributeValue(S = KeyPrefix.build PoolTable.Prefix.gambler gamblerId.Value) ]

        let updateExpression =
            $"ADD {ExpressionAttribute.name PoolTable.Attribute.score} :delta"

        let attributeNames = ExpressionAttribute.names [ PoolTable.Attribute.score ]

        let attributeValues = dict [ ":delta", AttributeValue(N = delta.ToString()) ]

        Update(
            TableName = PoolTable.name,
            Key = Dictionary key,
            UpdateExpression = updateExpression,
            ExpressionAttributeNames = Dictionary attributeNames,
            ExpressionAttributeValues = Dictionary attributeValues
        )
