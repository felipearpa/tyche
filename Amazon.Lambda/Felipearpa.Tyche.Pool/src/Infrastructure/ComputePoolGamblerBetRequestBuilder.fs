namespace Felipearpa.Tyche.Pool.Infrastructure

open System
open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Domain.BetEvaluator
open Felipearpa.Tyche.Pool.Type

module ComputePoolGamblerBetRequestBuilder =

    let build (poolGamblerBet: PoolGamblerBet) (matchScore: TeamScore<int>) (computedRequestId: String) =
        let pk =
            $"{KeyPrefix.build PoolTable.Prefix.gambler poolGamblerBet.GamblerId.Value}#{KeyPrefix.build PoolTable.Prefix.pool poolGamblerBet.PoolId.Value}"

        let sk = KeyPrefix.build PoolTable.Prefix.match' poolGamblerBet.MatchId.Value

        let key = dict [ Key.pk, AttributeValue(pk); Key.sk, AttributeValue(sk) ]

        let updateExpression =
            $"SET {ExpressionAttribute.name PoolTable.Attribute.homeTeamScore} = :{PoolTable.Attribute.homeTeamScore}, \
             {ExpressionAttribute.name PoolTable.Attribute.awayTeamScore} = :{PoolTable.Attribute.awayTeamScore}, \
             {ExpressionAttribute.name PoolTable.Attribute.score} = :delta, \
             {ExpressionAttribute.name PoolTable.Attribute.computedDateTime} = :now, \
             {ExpressionAttribute.name PoolTable.Attribute.computedRequestId} = :{PoolTable.Attribute.computedRequestId}"

        let conditionExpression =
            $"attribute_not_exists({ExpressionAttribute.name PoolTable.Attribute.computedRequestId})"

        let mutable attributeNames =
            ExpressionAttribute.names
                [ PoolTable.Attribute.homeTeamScore
                  PoolTable.Attribute.awayTeamScore
                  PoolTable.Attribute.score
                  PoolTable.Attribute.computedDateTime
                  PoolTable.Attribute.computedRequestId ]

        let mutable attributeValues =
            dict
                [ $":{PoolTable.Attribute.homeTeamScore}", AttributeValue(N = matchScore.HomeTeamValue.ToString())
                  $":{PoolTable.Attribute.awayTeamScore}", AttributeValue(N = matchScore.AwayTeamValue.ToString())
                  ":delta", AttributeValue(N = (delta poolGamblerBet.BetScore matchScore).ToString())
                  ":now", AttributeValue(S = DateTime.UtcNow.ToString("o"))
                  $":{PoolTable.Attribute.computedRequestId}", AttributeValue(S = computedRequestId) ]

        Update(
            TableName = PoolTable.name,
            Key = Dictionary key,
            ExpressionAttributeNames = Dictionary attributeNames,
            ExpressionAttributeValues = Dictionary attributeValues,
            UpdateExpression = updateExpression,
            ConditionExpression = conditionExpression
        )
