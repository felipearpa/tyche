namespace Felipearpa.Tyche.MatchScoreIngestion.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb
open Felipearpa.Type

module MarkExpiredRequestBuilder =

    let build (matchId: Ulid) =
        let key =
            dict [ Key.pk, AttributeValue(S = KeyPrefix.build MatchScoreIngestionTable.Prefix.match' matchId.Value) ]

        let attributeNames = ExpressionAttribute.names [ MatchScoreIngestionTable.Attribute.status ]

        let attributeValues =
            dict
                [ $":{MatchScoreIngestionTable.Attribute.status}",
                  AttributeValue(S = MatchScoreIngestionTable.Status.expired) ]

        let updateExpression =
            $"SET {ExpressionAttribute.name MatchScoreIngestionTable.Attribute.status} = :{MatchScoreIngestionTable.Attribute.status}"

        UpdateItemRequest(
            TableName = MatchScoreIngestionTable.name,
            Key = Dictionary key,
            UpdateExpression = updateExpression,
            ExpressionAttributeNames = Dictionary attributeNames,
            ExpressionAttributeValues = Dictionary attributeValues
        )
