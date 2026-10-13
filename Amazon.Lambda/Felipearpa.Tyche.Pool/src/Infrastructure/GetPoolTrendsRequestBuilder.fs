namespace Felipearpa.Tyche.Pool.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb
open Felipearpa.Type

/// Reads the trend state of pool roots: their layout and trend kickoff.
module GetPoolTrendsRequestBuilder =

    let build (poolIds: Ulid list) =
        let keys =
            poolIds
            |> List.map (fun poolId ->
                let poolKey = KeyPrefix.build PoolTable.Prefix.pool poolId.Value
                Dictionary(dict [ Key.pk, AttributeValue(S = poolKey); Key.sk, AttributeValue(S = poolKey) ]))

        let attributes =
            [ PoolTable.Attribute.poolId
              PoolTable.Attribute.poolLayoutId
              PoolTable.Attribute.trendKickoffDateTime ]

        BatchGetItemRequest(
            RequestItems =
                Dictionary(
                    dict
                        [ PoolTable.name,
                          KeysAndAttributes(
                              Keys = ResizeArray(keys),
                              ProjectionExpression = (attributes |> List.map ExpressionAttribute.name |> String.concat ", "),
                              ExpressionAttributeNames = Dictionary(ExpressionAttribute.names attributes)
                          ) ]
                )
        )
