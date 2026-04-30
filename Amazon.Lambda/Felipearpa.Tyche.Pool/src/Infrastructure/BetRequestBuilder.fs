namespace Felipearpa.Tyche.Pool.Infrastructure

open System
open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Type
open Felipearpa.Type

module BetRequestBuilder =

    let build (poolId: Ulid) (gamblerId: Ulid) (matchId: Ulid) (betScore: TeamScore<BetScore>) =
        let key =
            dict
                [ Key.pk,
                  AttributeValue(
                      $"{KeyPrefix.build PoolTable.Prefix.gambler gamblerId.Value}#{KeyPrefix.build PoolTable.Prefix.pool poolId.Value}"
                  )
                  Key.sk, AttributeValue(KeyPrefix.build PoolTable.Prefix.match' matchId.Value) ]

        let updateExpression =
            $"SET {ExpressionAttribute.name PoolTable.Attribute.homeTeamBet} = :{PoolTable.Attribute.homeTeamBet}, \
             {ExpressionAttribute.name PoolTable.Attribute.awayTeamBet} = :{PoolTable.Attribute.awayTeamBet}"

        let conditionExpression =
            $":lockHorizon < {ExpressionAttribute.name PoolTable.Attribute.matchDateTime}"

        let mutable attributeNames =
            ExpressionAttribute.names
                [ PoolTable.Attribute.homeTeamBet
                  PoolTable.Attribute.awayTeamBet
                  PoolTable.Attribute.matchDateTime ]

        let lockHorizon = DateTime.UtcNow + LockPolicy.lockOffset

        let mutable attributeValues =
            dict
                [ $":{PoolTable.Attribute.homeTeamBet}", AttributeValue(N = betScore.HomeTeamValue.ToString())
                  $":{PoolTable.Attribute.awayTeamBet}", AttributeValue(N = betScore.AwayTeamValue.ToString())
                  ":lockHorizon", AttributeValue(S = lockHorizon.ToString("o")) ]

        UpdateItemRequest(
            TableName = PoolTable.name,
            Key = Dictionary key,
            ExpressionAttributeNames = Dictionary attributeNames,
            ExpressionAttributeValues = Dictionary attributeValues,
            UpdateExpression = updateExpression,
            ConditionExpression = conditionExpression,
            ReturnValues = "ALL_NEW"
        )
