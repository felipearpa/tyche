namespace Felipearpa.Tyche.Pool.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Type

module GetPoolGamblerScoreRequestBuilder =
    [<Literal>]
    let private tableName = "Pool"

    [<Literal>]
    let poolText = "POOL"

    [<Literal>]
    let private gamblerText = "GAMBLER"

    let build (poolId: Ulid) (gamblerId: Ulid) =
        let keyConditionExpression = "#pk = :pk and #sk = :sk"
        let filterConditionExpression = "#status = :status"

        let attributeValues =
            dict
                [ ":pk", AttributeValue($"{poolText}#{poolId.Value}")
                  ":sk", AttributeValue($"{gamblerText}#{gamblerId.Value}")
                  ":status", AttributeValue("OPENED") ]

        let attributeNames = dict [ "#pk", "pk"; "#sk", "sk"; "#status", "status" ]

        QueryRequest(
            TableName = tableName,
            KeyConditionExpression = keyConditionExpression,
            FilterExpression = filterConditionExpression,
            ExpressionAttributeNames = Dictionary attributeNames,
            ExpressionAttributeValues = Dictionary attributeValues
        )
