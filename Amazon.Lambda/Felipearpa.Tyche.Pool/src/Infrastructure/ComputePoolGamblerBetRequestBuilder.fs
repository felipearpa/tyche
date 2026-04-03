namespace Felipearpa.Tyche.Pool.Infrastructure

open System
open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Domain.BetEvaluator
open Felipearpa.Tyche.Pool.Type

module ComputePoolGamblerBetRequestBuilder =

    [<Literal>]
    let private tableName = "Pool"

    [<Literal>]
    let private matchKeyPrefix = "MATCH"

    [<Literal>]
    let private poolKeyPrefix = "POOL"

    [<Literal>]
    let private gamblerKeyPrefix = "GAMBLER"

    let build (poolGamblerBet: PoolGamblerBet) (matchScore: TeamScore<int>) (requestId: String) =
        let pk = $"{matchKeyPrefix}#{poolGamblerBet.MatchId}"

        let sk =
            $"{poolKeyPrefix}#{poolGamblerBet.PoolId}#{gamblerKeyPrefix}#{poolGamblerBet.GamblerId}"

        let key = dict [ "pk", AttributeValue(pk); "sk", AttributeValue(sk) ]

        let updateExpression =
            "SET #homeTeamScore = :homeTeamScore, \
             #awayTeamScore = :awayTeamScore, \
             #score = if_not_exists(#score, 0) + :delta, \
             #beforePosition = #currentPosition, \
             #delta = :delta, \
             #computedAt = :now, \
             #requestId = :requestId"

        let conditionExpression =
            "attribute_not_exists(#computedAt) AND attribute_not_exists(#requestId)"

        let mutable attributeNames =
            dict
                [ "#homeTeamScore", "homeTeamScore"
                  "#awayTeamScore", "awayTeamScore"
                  "#score", "score"
                  "#currentPosition", "currentPosition"
                  "#beforePosition", "beforePosition"
                  "#delta", "delta"
                  "#computedAt", "computedAt"
                  "#requestId", "requestId" ]

        let mutable attributeValues =
            dict
                [ ":homeTeamScore", AttributeValue(N = matchScore.HomeTeamValue.ToString())
                  ":awayTeamScore", AttributeValue(N = matchScore.AwayTeamValue.ToString())
                  ":delta", AttributeValue(N = (delta poolGamblerBet.BetScore matchScore).ToString())
                  ":now", AttributeValue(S = DateTime.UtcNow.ToString("o"))
                  ":requestId", AttributeValue(S = requestId) ]

        UpdateItemRequest(
            TableName = tableName,
            Key = Dictionary key,
            ExpressionAttributeNames = Dictionary attributeNames,
            ExpressionAttributeValues = Dictionary attributeValues,
            UpdateExpression = updateExpression,
            ConditionExpression = conditionExpression,
            ReturnValues = "ALL_NEW"
        )
