namespace Felipearpa.Tyche.PoolLayout.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Type

module GetPendingPoolLayoutMatchesRequestBuilder =
    [<Literal>]
    let private tableName = "PoolLayout"

    [<Literal>]
    let poolText = "POOLLAYOUT"

    [<Literal>]
    let private matchText = "MATCH"

    let build (poolLayoutId: Ulid) (poolLayoutVersion: int) (maybeNext: IDictionary<string, AttributeValue> option) =
        let keyConditionExpression = "#pk = :pk AND begins_with(#sk, :matchPrefix)"
        let filterExpression = "#poolLayoutVersion <= :poolLayoutVersion"

        let attributeValues =
            dict
                [ ":pk", AttributeValue(S = $"{poolText}#{poolLayoutId}")
                  ":matchPrefix", AttributeValue(S = $"{matchText}#")
                  ":poolLayoutVersion", AttributeValue(N = poolLayoutVersion.ToString()) ]

        let attributeNames =
            dict [ "#pk", "pk"; "#sk", "sk"; "#poolLayoutVersion", "poolLayoutVersion" ]

        QueryRequest(
            TableName = tableName,
            KeyConditionExpression = keyConditionExpression,
            FilterExpression = filterExpression,
            ExpressionAttributeNames = Dictionary attributeNames,
            ExpressionAttributeValues = Dictionary attributeValues,
            ExclusiveStartKey =
                match maybeNext with
                | None -> null
                | Some next -> Dictionary next
        )
