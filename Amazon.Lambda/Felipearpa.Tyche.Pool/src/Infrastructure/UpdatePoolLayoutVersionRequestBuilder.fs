namespace Felipearpa.Tyche.Pool.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb
open Felipearpa.Type

module UpdatePoolLayoutVersionRequestBuilder =

    let build (poolId: Ulid) (gamblerId: Ulid) (newPoolLayoutVersion: int) =
        let pk = KeyPrefix.build PoolTable.Prefix.pool poolId.Value
        let sk = KeyPrefix.build PoolTable.Prefix.gambler gamblerId.Value

        let key = dict [ Key.pk, AttributeValue(S = pk); Key.sk, AttributeValue(S = sk) ]

        let updateExpression =
            $"SET {ExpressionAttribute.name PoolTable.Attribute.poolLayoutVersion} = :{PoolTable.Attribute.poolLayoutVersion}"

        let conditionExpression =
            $"{ExpressionAttribute.name PoolTable.Attribute.poolLayoutVersion} < :{PoolTable.Attribute.poolLayoutVersion}"

        let attributeNames =
            ExpressionAttribute.names [ PoolTable.Attribute.poolLayoutVersion ]

        let attributeValues =
            dict
                [ $":{PoolTable.Attribute.poolLayoutVersion}",
                  AttributeValue(N = newPoolLayoutVersion.ToString()) ]

        UpdateItemRequest(
            TableName = PoolTable.name,
            Key = Dictionary key,
            UpdateExpression = updateExpression,
            ConditionExpression = conditionExpression,
            ExpressionAttributeNames = Dictionary attributeNames,
            ExpressionAttributeValues = Dictionary attributeValues
        )
