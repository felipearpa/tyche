namespace Felipearpa.Tyche.Pool.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb
open Felipearpa.Type

module UpdateMatchPositionRequestBuilder =

    let build (poolId: Ulid) (gamblerId: Ulid) (matchId: Ulid) (currentPosition: int) (beforePosition: int) =
        let pk =
            $"{KeyPrefix.build PoolTable.Prefix.gambler gamblerId.Value}#{KeyPrefix.build PoolTable.Prefix.pool poolId.Value}"

        let sk = KeyPrefix.build PoolTable.Prefix.match' matchId.Value

        let key = dict [ Key.pk, AttributeValue(pk); Key.sk, AttributeValue(sk) ]

        let updateExpression =
            $"SET {ExpressionAttribute.name PoolTable.Attribute.currentPosition} = :{PoolTable.Attribute.currentPosition}, \
             {ExpressionAttribute.name PoolTable.Attribute.beforePosition} = :{PoolTable.Attribute.beforePosition}"

        let attributeNames =
            ExpressionAttribute.names [ PoolTable.Attribute.currentPosition; PoolTable.Attribute.beforePosition ]

        let attributeValues =
            dict
                [ $":{PoolTable.Attribute.currentPosition}", AttributeValue(N = currentPosition.ToString())
                  $":{PoolTable.Attribute.beforePosition}", AttributeValue(N = beforePosition.ToString()) ]

        UpdateItemRequest(
            TableName = PoolTable.name,
            Key = Dictionary key,
            UpdateExpression = updateExpression,
            ExpressionAttributeNames = Dictionary attributeNames,
            ExpressionAttributeValues = Dictionary attributeValues
        )
