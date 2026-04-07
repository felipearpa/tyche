namespace Felipearpa.Tyche.Pool.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb
open Felipearpa.Type

module GetGamblerScoresRequestBuilder =

    let build (gamblerId: Ulid) (maybeNext: IDictionary<string, AttributeValue> option) =
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
                | Some next -> Dictionary next
        )
