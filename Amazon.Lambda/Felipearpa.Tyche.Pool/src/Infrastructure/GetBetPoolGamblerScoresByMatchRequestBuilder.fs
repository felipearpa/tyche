namespace Felipearpa.Tyche.Pool.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb
open Felipearpa.Type

module GetBetPoolGamblerScoresByMatchRequestBuilder =

    let build (matchId: Ulid) (maybeNext: IDictionary<string, AttributeValue> option) =
        let keyConditionExpression =
            $"{ExpressionAttribute.name PoolTable.Attribute.getPoolGamblerScoresByMatchPk} = :{PoolTable.Attribute.getPoolGamblerScoresByMatchPk}"

        let filterExpression =
            $"attribute_not_exists({ExpressionAttribute.name PoolTable.Attribute.computedRequestId}) \
             AND attribute_exists({ExpressionAttribute.name PoolTable.Attribute.homeTeamBet}) \
             AND attribute_exists({ExpressionAttribute.name PoolTable.Attribute.awayTeamBet})"

        let attributeValues =
            dict
                [ $":{PoolTable.Attribute.getPoolGamblerScoresByMatchPk}",
                  AttributeValue(S = KeyPrefix.build PoolTable.Prefix.match' matchId.Value) ]

        let attributeNames =
            ExpressionAttribute.names
                [ PoolTable.Attribute.getPoolGamblerScoresByMatchPk
                  PoolTable.Attribute.computedRequestId
                  PoolTable.Attribute.homeTeamBet
                  PoolTable.Attribute.awayTeamBet ]

        QueryRequest(
            TableName = PoolTable.name,
            IndexName = PoolTable.Index.scoresByMatch,
            KeyConditionExpression = keyConditionExpression,
            FilterExpression = filterExpression,
            ExpressionAttributeNames = Dictionary attributeNames,
            ExpressionAttributeValues = Dictionary attributeValues,
            ExclusiveStartKey =
                match maybeNext with
                | None -> null
                | Some next -> Dictionary next
        )
