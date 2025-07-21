namespace Felipearpa.Tyche.PoolLayout.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model

module GetOpenPoolLayoutRequestBuilder =
    [<Literal>]
    let private tableName = "PoolLayout"

    [<Literal>]
    let private indexName = "GetOpenedPoolLayout-index"

    [<Literal>]
    let poolText = "POOLLAYOUT"

    [<Literal>]
    let private gamblerText = "GAMBLER"

    let build (maybeNext: IDictionary<string, AttributeValue> option) =
        let keyConditionExpression = "#status = :status"

        let attributeValues = dict [ ":status", AttributeValue("OPENED") ]

        let attributeNames = dict [ "#status", "status" ]

        QueryRequest(
            TableName = tableName,
            IndexName = indexName,
            KeyConditionExpression = keyConditionExpression,
            ExpressionAttributeNames = Dictionary attributeNames,
            ExpressionAttributeValues = Dictionary attributeValues,
            ExclusiveStartKey =
                match maybeNext with
                | None -> null
                | Some next -> Dictionary next
        )
