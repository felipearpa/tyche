namespace Felipearpa.Tyche.Pool.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb
open Felipearpa.Type

module IncrementGamblerCountRequestBuilder =

    let build (poolId: Ulid) =
        let poolKey = KeyPrefix.build PoolTable.Prefix.pool poolId.Value

        let key =
            dict [ Key.pk, AttributeValue(S = poolKey); Key.sk, AttributeValue(S = poolKey) ]

        // Every join or removal also advances the membership revision that position
        // calculations use to detect a membership change while they read.
        let updateExpression =
            $"SET {ExpressionAttribute.name PoolTable.Attribute.gamblerCount} = \
             if_not_exists({ExpressionAttribute.name PoolTable.Attribute.gamblerCount}, :zero) + :one, \
             {ExpressionAttribute.name PoolTable.Attribute.membershipRevision} = \
             if_not_exists({ExpressionAttribute.name PoolTable.Attribute.membershipRevision}, :zero) + :one"

        let attributeNames =
            ExpressionAttribute.names [ PoolTable.Attribute.gamblerCount; PoolTable.Attribute.membershipRevision ]

        let attributeValues =
            dict [ ":zero", AttributeValue(N = "0"); ":one", AttributeValue(N = "1") ]

        Update(
            TableName = PoolTable.name,
            Key = Dictionary key,
            UpdateExpression = updateExpression,
            ExpressionAttributeNames = Dictionary attributeNames,
            ExpressionAttributeValues = Dictionary attributeValues
        )
