namespace Felipearpa.Tyche.Pool.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb
open Felipearpa.Type

module GetGamblerScoresRequestBuilder =

    /// The start key of the scores-by-gambler index: a membership's table keys and its score.
    let startKeyContract (gamblerId: Ulid) : StartKeyContract =
        Map
            [ Key.pk, StringStartingWith(KeyPrefix.build PoolTable.Prefix.pool "")
              Key.sk, StringEqualTo(KeyPrefix.build PoolTable.Prefix.gambler gamblerId.Value)
              PoolTable.Attribute.score, AnyNumber ]

    let build
        (gamblerId: Ulid)
        (maybeNext: string option)
        (deserialize: string -> IDictionary<string, AttributeValue>)
        =
        let keyConditionExpression = $"{ExpressionAttribute.name Key.sk} = :sk"

        let filterExpression =
            $"{ExpressionAttribute.name PoolTable.Attribute.status} = :status"

        let attributeValues =
            dict
                [ ":sk", AttributeValue(KeyPrefix.build PoolTable.Prefix.gambler gamblerId.Value)
                  ":status", AttributeValue("OPENED") ]

        let attributeNames =
            ExpressionAttribute.names [ Key.sk; PoolTable.Attribute.status ]

        QueryRequest(
            TableName = PoolTable.name,
            IndexName = PoolTable.Index.scoresByGambler,
            ScanIndexForward = false,
            KeyConditionExpression = keyConditionExpression,
            FilterExpression = filterExpression,
            ExpressionAttributeNames = Dictionary attributeNames,
            ExpressionAttributeValues = Dictionary attributeValues,
            ExclusiveStartKey =
                match maybeNext with
                | None -> null
                | Some next -> StartKeyContract.startKey (startKeyContract gamblerId) deserialize next
        )
