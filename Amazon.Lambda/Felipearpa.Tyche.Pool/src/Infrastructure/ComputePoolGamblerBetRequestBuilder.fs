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

    let build (poolGamblerBet: PoolGamblerBet) (matchScore: TeamScore<int>) (computedRequestId: String) =
        let pk =
            $"{gamblerKeyPrefix}#{poolGamblerBet.GamblerId}#{poolKeyPrefix}#{poolGamblerBet.PoolId}"

        let sk = $"{matchKeyPrefix}#{poolGamblerBet.MatchId}"

        let key = dict [ "pk", AttributeValue(pk); "sk", AttributeValue(sk) ]

        let updateExpression =
            "SET #homeTeamScore = :homeTeamScore, \
             #awayTeamScore = :awayTeamScore, \
             #score = :delta, \
             #computedDateTime = :now, \
             #computedRequestId = :computedRequestId"

        let conditionExpression = "attribute_not_exists(#computedRequestId)"

        let mutable attributeNames =
            dict
                [ "#homeTeamScore", "homeTeamScore"
                  "#awayTeamScore", "awayTeamScore"
                  "#score", "score"
                  "#computedDateTime", "computedDateTime"
                  "#computedRequestId", "computedRequestId" ]

        let mutable attributeValues =
            dict
                [ ":homeTeamScore", AttributeValue(N = matchScore.HomeTeamValue.ToString())
                  ":awayTeamScore", AttributeValue(N = matchScore.AwayTeamValue.ToString())
                  ":delta", AttributeValue(N = (delta poolGamblerBet.BetScore matchScore).ToString())
                  ":now", AttributeValue(S = DateTime.UtcNow.ToString("o"))
                  ":computedRequestId", AttributeValue(S = computedRequestId) ]

        Update(
            TableName = tableName,
            Key = Dictionary key,
            ExpressionAttributeNames = Dictionary attributeNames,
            ExpressionAttributeValues = Dictionary attributeValues,
            UpdateExpression = updateExpression,
            ConditionExpression = conditionExpression
        )
