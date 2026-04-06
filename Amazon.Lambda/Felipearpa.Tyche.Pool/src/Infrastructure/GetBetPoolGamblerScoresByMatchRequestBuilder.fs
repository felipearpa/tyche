namespace Felipearpa.Tyche.Pool.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Type

module GetBetPoolGamblerScoresByMatchRequestBuilder =

    [<Literal>]
    let private tableName = "Pool"

    [<Literal>]
    let private matchKeyPrefix = "MATCH"

    let build (matchId: Ulid) (maybeNext: IDictionary<string, AttributeValue> option) =
        let keyConditionExpression =
            "#getPoolGamblerScoresByMatchPk = :getPoolGamblerScoresByMatchPk"

        let filterExpression =
            "attribute_not_exists(#computedAt) AND attribute_exists(#homeTeamBet) AND attribute_exists(#awayTeamBet)"

        let attributeValues =
            dict [ ":getPoolGamblerScoresByMatchPk", AttributeValue(S = $"{matchKeyPrefix}#{matchId}") ]

        let attributeNames =
            dict
                [ "#getPoolGamblerScoresByMatchPk", "getPoolGamblerScoresByMatchPk"
                  "#computedAt", "computedAt"
                  "#homeTeamBet", "homeTeamBet"
                  "#awayTeamBet", "awayTeamBet" ]

        QueryRequest(
            TableName = tableName,
            IndexName = "GetPoolGamblerScoresByMatch-index",
            KeyConditionExpression = keyConditionExpression,
            FilterExpression = filterExpression,
            ExpressionAttributeNames = Dictionary attributeNames,
            ExpressionAttributeValues = Dictionary attributeValues,
            ExclusiveStartKey =
                match maybeNext with
                | None -> null
                | Some next -> Dictionary next
        )
