namespace Felipearpa.Tyche.Pool.Infrastructure

open System
open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

module GetPoolMatchGamblerBetsRequestBuilder =

    let build
        (poolId: Ulid)
        (matchId: Ulid)
        (maybeNext: string option)
        (deserialize: string -> IDictionary<string, AttributeValue>)
        =
        let lockHorizon = DateTime.UtcNow + LockPolicy.lockOffset

        let keyConditionExpression =
            $"{ExpressionAttribute.name PoolTable.Attribute.getPoolGamblerScoresByMatchPk} = :pk \
             and begins_with({ExpressionAttribute.name PoolTable.Attribute.getPoolGamblerScoresByMatchSk}, :poolPrefix)"

        let filterExpression =
            $"attribute_exists({ExpressionAttribute.name PoolTable.Attribute.homeTeamBet}) \
             AND attribute_exists({ExpressionAttribute.name PoolTable.Attribute.awayTeamBet}) \
             AND {ExpressionAttribute.name PoolTable.Attribute.matchDateTime} <= :lockHorizon"

        let attributeValues =
            dict
                [ ":pk", AttributeValue(S = KeyPrefix.build PoolTable.Prefix.match' matchId.Value)
                  ":poolPrefix", AttributeValue(S = $"{KeyPrefix.build PoolTable.Prefix.pool poolId.Value}#")
                  ":lockHorizon", AttributeValue(S = lockHorizon.ToString("o")) ]

        let attributeNames =
            ExpressionAttribute.names
                [ PoolTable.Attribute.getPoolGamblerScoresByMatchPk
                  PoolTable.Attribute.getPoolGamblerScoresByMatchSk
                  PoolTable.Attribute.homeTeamBet
                  PoolTable.Attribute.awayTeamBet
                  PoolTable.Attribute.matchDateTime ]

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
                | Some next -> Dictionary(next |> deserialize)
        )
