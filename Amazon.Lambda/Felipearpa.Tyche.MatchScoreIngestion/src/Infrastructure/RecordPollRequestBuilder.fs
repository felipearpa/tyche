namespace Felipearpa.Tyche.MatchScoreIngestion.Infrastructure

open System
open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb
open Felipearpa.Type

module RecordPollRequestBuilder =

    let build (matchId: Ulid) (polledAt: DateTime) =
        let key =
            dict [ Key.pk, AttributeValue(S = KeyPrefix.build MatchScoreIngestionTable.Prefix.match' matchId.Value) ]

        let attributeNames =
            ExpressionAttribute.names
                [ MatchScoreIngestionTable.Attribute.lastPolledDateTime
                  MatchScoreIngestionTable.Attribute.pollCount ]

        let attributeValues =
            dict
                [ $":{MatchScoreIngestionTable.Attribute.lastPolledDateTime}", AttributeValue(S = polledAt.ToString("O"))
                  ":increment", AttributeValue(N = "1") ]

        let updateExpression =
            $"SET {ExpressionAttribute.name MatchScoreIngestionTable.Attribute.lastPolledDateTime} = :{MatchScoreIngestionTable.Attribute.lastPolledDateTime} ADD {ExpressionAttribute.name MatchScoreIngestionTable.Attribute.pollCount} :increment"

        UpdateItemRequest(
            TableName = MatchScoreIngestionTable.name,
            Key = Dictionary key,
            UpdateExpression = updateExpression,
            ExpressionAttributeNames = Dictionary attributeNames,
            ExpressionAttributeValues = Dictionary attributeValues
        )
