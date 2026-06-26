namespace Felipearpa.Tyche.MatchScoreIngestion.Infrastructure

open System
open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb
open Felipearpa.Type

module MarkCompletedRequestBuilder =

    let build (matchId: Ulid) (effectiveAt: DateTime) =
        let key =
            dict [ Key.pk, AttributeValue(S = KeyPrefix.build MatchScoreIngestionTable.Prefix.match' matchId.Value) ]

        let attributeNames =
            ExpressionAttribute.names
                [ MatchScoreIngestionTable.Attribute.status
                  MatchScoreIngestionTable.Attribute.effectiveDateTime ]

        let attributeValues =
            dict
                [ $":{MatchScoreIngestionTable.Attribute.status}",
                  AttributeValue(S = MatchScoreIngestionTable.Status.completed)
                  $":{MatchScoreIngestionTable.Attribute.effectiveDateTime}", AttributeValue(S = effectiveAt.ToString("O")) ]

        let updateExpression =
            $"SET {ExpressionAttribute.name MatchScoreIngestionTable.Attribute.status} = :{MatchScoreIngestionTable.Attribute.status}, {ExpressionAttribute.name MatchScoreIngestionTable.Attribute.effectiveDateTime} = :{MatchScoreIngestionTable.Attribute.effectiveDateTime}"

        UpdateItemRequest(
            TableName = MatchScoreIngestionTable.name,
            Key = Dictionary key,
            UpdateExpression = updateExpression,
            ExpressionAttributeNames = Dictionary attributeNames,
            ExpressionAttributeValues = Dictionary attributeValues
        )
