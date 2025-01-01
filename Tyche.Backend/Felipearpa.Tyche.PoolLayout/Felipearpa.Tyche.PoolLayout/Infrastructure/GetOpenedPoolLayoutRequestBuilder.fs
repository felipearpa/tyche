namespace Felipearpa.Tyche.PoolLayout.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model

module GetOpenedPoolLayoutRequestBuilder =
    [<Literal>]
    let private tableName = "PoolLayout"

    [<Literal>]
    let private indexName = "status-startDateTime-index"

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
