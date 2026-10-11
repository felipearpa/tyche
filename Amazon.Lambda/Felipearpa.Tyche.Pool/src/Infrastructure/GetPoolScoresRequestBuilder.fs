namespace Felipearpa.Tyche.Pool.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb
open Felipearpa.Type

module GetPoolScoresRequestBuilder =

    /// The start key of the scores-by-pool index: a membership's table keys and its score.
    let startKeyContract (poolId: Ulid) : StartKeyContract =
        Map
            [ Key.pk, StringEqualTo(KeyPrefix.build PoolTable.Prefix.pool poolId.Value)
              Key.sk, StringStartingWith(KeyPrefix.build PoolTable.Prefix.gambler "")
              PoolTable.Attribute.score, AnyNumber ]

    let build
        (poolId: Ulid)
        (maybeNext: string option)
        (deserialize: string -> IDictionary<string, AttributeValue>)
        =
        let keyConditionExpression = $"{ExpressionAttribute.name Key.pk} = :pk"

        let filterExpression =
            $"{ExpressionAttribute.name PoolTable.Attribute.status} = :status"

        let attributeValues =
            dict
                [ ":pk", AttributeValue(KeyPrefix.build PoolTable.Prefix.pool poolId.Value)
                  ":status", AttributeValue("OPENED") ]

        let attributeNames =
            ExpressionAttribute.names [ Key.pk; PoolTable.Attribute.status ]

        QueryRequest(
            TableName = PoolTable.name,
            IndexName = PoolTable.Index.scoresByPool,
            ScanIndexForward = false,
            KeyConditionExpression = keyConditionExpression,
            FilterExpression = filterExpression,
            ExpressionAttributeNames = Dictionary attributeNames,
            ExpressionAttributeValues = Dictionary attributeValues,
            ExclusiveStartKey =
                match maybeNext with
                | None -> null
                | Some next -> StartKeyContract.startKey (startKeyContract poolId) deserialize next
        )
