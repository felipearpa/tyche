namespace Felipearpa.Tyche.Pool.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb
open Felipearpa.Type

module IsPoolMemberRequestBuilder =

    let build (poolId: Ulid) (gamblerId: Ulid) =
        let key =
            dict
                [ Key.pk, AttributeValue(S = KeyPrefix.build PoolTable.Prefix.pool poolId.Value)
                  Key.sk, AttributeValue(S = KeyPrefix.build PoolTable.Prefix.gambler gamblerId.Value) ]

        GetItemRequest(
            TableName = PoolTable.name,
            Key = Dictionary key,
            ProjectionExpression = ExpressionAttribute.name Key.pk,
            ExpressionAttributeNames = Dictionary(ExpressionAttribute.names [ Key.pk ])
        )
