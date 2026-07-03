namespace Felipearpa.Tyche.Pool.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb
open Felipearpa.Type

module GetGamblerCountsRequestBuilder =

    let build (poolIds: Ulid list) =
        let keys =
            poolIds
            |> List.map (fun poolId ->
                let poolKey = KeyPrefix.build PoolTable.Prefix.pool poolId.Value

                dict [ Key.pk, AttributeValue(S = poolKey); Key.sk, AttributeValue(S = poolKey) ]
                |> Dictionary)

        let projectionExpression =
            $"{ExpressionAttribute.name PoolTable.Attribute.poolId}, {ExpressionAttribute.name PoolTable.Attribute.gamblerCount}"

        let attributeNames =
            ExpressionAttribute.names [ PoolTable.Attribute.poolId; PoolTable.Attribute.gamblerCount ]

        BatchGetItemRequest(
            RequestItems =
                Dictionary(
                    dict
                        [ PoolTable.name,
                          KeysAndAttributes(
                              Keys = ResizeArray(keys),
                              ProjectionExpression = projectionExpression,
                              ExpressionAttributeNames = Dictionary attributeNames
                          ) ]
                )
        )
