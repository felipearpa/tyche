namespace Felipearpa.Tyche.Pool.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Type

module GetPoolScoresRequestBuilder =
    [<Literal>]
    let private tableName = "Pool"

    [<Literal>]
    let poolText = "POOL"

    let build (poolId: Ulid) (maybeNext: IDictionary<string, AttributeValue> option) =
        let keyConditionExpression = "#pk = :pk"

        let filterExpression = "#status = :status"

        let attributeValues =
            dict
                [ ":pk", AttributeValue($"{poolText}#{poolId.Value}")
                  ":status", AttributeValue("OPENED") ]

        let attributeNames = dict [ "#pk", "pk"; "#status", "status" ]

        QueryRequest(
            TableName = tableName,
            IndexName = "GetPoolGamblerScoresByPool-index",
            KeyConditionExpression = keyConditionExpression,
            FilterExpression = filterExpression,
            ExpressionAttributeNames = Dictionary attributeNames,
            ExpressionAttributeValues = Dictionary attributeValues,
            ExclusiveStartKey =
                match maybeNext with
                | None -> null
                | Some next -> Dictionary next
        )
