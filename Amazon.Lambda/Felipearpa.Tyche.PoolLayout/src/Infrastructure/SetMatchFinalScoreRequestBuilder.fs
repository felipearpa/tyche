namespace Felipearpa.Tyche.PoolLayout.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb
open Felipearpa.Type

module SetMatchFinalScoreRequestBuilder =

    let build (poolLayoutId: Ulid) (matchId: Ulid) (homeTeamScore: int) (awayTeamScore: int) =
        let key =
            dict
                [ Key.pk, AttributeValue(S = KeyPrefix.build PoolLayoutTable.Prefix.poolLayout poolLayoutId.Value)
                  Key.sk, AttributeValue(S = KeyPrefix.build PoolLayoutTable.Prefix.match' matchId.Value) ]

        let attributeNames =
            ExpressionAttribute.names
                [ PoolLayoutTable.Attribute.homeTeamScore
                  PoolLayoutTable.Attribute.awayTeamScore ]

        let attributeValues =
            dict
                [ $":{PoolLayoutTable.Attribute.homeTeamScore}", AttributeValue(N = homeTeamScore.ToString())
                  $":{PoolLayoutTable.Attribute.awayTeamScore}", AttributeValue(N = awayTeamScore.ToString()) ]

        let updateExpression =
            $"SET {ExpressionAttribute.name PoolLayoutTable.Attribute.homeTeamScore} = :{PoolLayoutTable.Attribute.homeTeamScore}, {ExpressionAttribute.name PoolLayoutTable.Attribute.awayTeamScore} = :{PoolLayoutTable.Attribute.awayTeamScore}"

        // Idempotent: only the first write wins, so re-runs after the score is set are no-ops.
        let conditionExpression =
            $"attribute_not_exists({ExpressionAttribute.name PoolLayoutTable.Attribute.homeTeamScore})"

        UpdateItemRequest(
            TableName = PoolLayoutTable.name,
            Key = Dictionary key,
            UpdateExpression = updateExpression,
            ConditionExpression = conditionExpression,
            ExpressionAttributeNames = Dictionary attributeNames,
            ExpressionAttributeValues = Dictionary attributeValues
        )
