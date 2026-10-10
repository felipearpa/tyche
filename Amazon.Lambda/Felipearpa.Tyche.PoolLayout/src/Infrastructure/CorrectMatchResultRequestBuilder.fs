namespace Felipearpa.Tyche.PoolLayout.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb
open Felipearpa.Type

/// Publishes a corrected result as the next result version. The condition pins the version
/// being corrected, so concurrent or repeated corrections cannot skip or reuse a version.
module CorrectMatchResultRequestBuilder =

    let build (poolLayoutId: Ulid) (matchId: Ulid) (homeTeamScore: int) (awayTeamScore: int) (expectedVersion: int) =
        let key =
            dict
                [ Key.pk, AttributeValue(S = KeyPrefix.build PoolLayoutTable.Prefix.poolLayout poolLayoutId.Value)
                  Key.sk, AttributeValue(S = KeyPrefix.build PoolLayoutTable.Prefix.match' matchId.Value) ]

        let home = ExpressionAttribute.name PoolLayoutTable.Attribute.homeTeamScore
        let away = ExpressionAttribute.name PoolLayoutTable.Attribute.awayTeamScore
        let version = ExpressionAttribute.name PoolLayoutTable.Attribute.resultVersion

        // A result published before versioning is version 1 without the attribute.
        let versionCondition =
            if expectedVersion = 1 then
                $"({version} = :expectedVersion OR attribute_not_exists({version}))"
            else
                $"{version} = :expectedVersion"

        UpdateItemRequest(
            TableName = PoolLayoutTable.name,
            Key = Dictionary key,
            UpdateExpression = $"SET {home} = :home, {away} = :away, {version} = :nextVersion",
            ConditionExpression = $"attribute_exists({home}) AND attribute_exists({away}) AND {versionCondition}",
            ExpressionAttributeNames =
                Dictionary(
                    ExpressionAttribute.names
                        [ PoolLayoutTable.Attribute.homeTeamScore
                          PoolLayoutTable.Attribute.awayTeamScore
                          PoolLayoutTable.Attribute.resultVersion ]
                ),
            ExpressionAttributeValues =
                Dictionary(
                    dict
                        [ ":home", AttributeValue(N = string homeTeamScore)
                          ":away", AttributeValue(N = string awayTeamScore)
                          ":expectedVersion", AttributeValue(N = string expectedVersion)
                          ":nextVersion", AttributeValue(N = string (expectedVersion + 1)) ]
                )
        )
