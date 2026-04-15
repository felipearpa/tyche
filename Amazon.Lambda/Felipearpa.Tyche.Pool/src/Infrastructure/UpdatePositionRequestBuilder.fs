namespace Felipearpa.Tyche.Pool.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb
open Felipearpa.Type

module UpdatePositionRequestBuilder =

    let build (poolId: Ulid) (gamblerId: Ulid) (position: int) =
        let key =
            dict
                [ Key.pk, AttributeValue(S = KeyPrefix.build PoolTable.Prefix.pool poolId.Value)
                  Key.sk, AttributeValue(S = KeyPrefix.build PoolTable.Prefix.gambler gamblerId.Value) ]

        let updateExpression =
            $"SET {ExpressionAttribute.name PoolTable.Attribute.beforePosition} = {ExpressionAttribute.name PoolTable.Attribute.position}, \
             {ExpressionAttribute.name PoolTable.Attribute.position} = :{PoolTable.Attribute.position}"

        let attributeNames =
            ExpressionAttribute.names [ PoolTable.Attribute.position; PoolTable.Attribute.beforePosition ]

        let attributeValues =
            dict [ $":{PoolTable.Attribute.position}", AttributeValue(N = position.ToString()) ]

        UpdateItemRequest(
            TableName = PoolTable.name,
            Key = Dictionary key,
            UpdateExpression = updateExpression,
            ExpressionAttributeNames = Dictionary attributeNames,
            ExpressionAttributeValues = Dictionary attributeValues
        )
