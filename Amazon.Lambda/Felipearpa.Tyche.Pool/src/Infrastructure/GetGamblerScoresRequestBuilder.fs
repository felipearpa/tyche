namespace Felipearpa.Tyche.Pool.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Type

module GetGamblerScoresRequestBuilder =
    [<Literal>]
    let private tableName = "Pool"

    [<Literal>]
    let private gamblerText = "GAMBLER"

    let build (gamblerId: Ulid) (maybeNext: IDictionary<string, AttributeValue> option) =
        let keyConditionExpression = "#sk = :sk"

        let filterExpression = "#status = :status"

        let attributeValues =
            dict
                [ ":sk", AttributeValue($"{gamblerText}#{gamblerId.Value}")
                  ":status", AttributeValue("OPENED") ]

        let attributeNames = dict [ "#sk", "sk"; "#status", "status" ]

        QueryRequest(
            TableName = tableName,
            IndexName = "GetPoolGamblerScoresByGambler-index",
            KeyConditionExpression = keyConditionExpression,
            FilterExpression = filterExpression,
            ExpressionAttributeNames = Dictionary attributeNames,
            ExpressionAttributeValues = Dictionary attributeValues,
            ExclusiveStartKey =
                match maybeNext with
                | None -> null
                | Some next -> Dictionary next
        )
