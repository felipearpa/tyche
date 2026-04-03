namespace Felipearpa.Tyche.Pool.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Type

module UpdateCurrentPositionRequestBuilder =

    [<Literal>]
    let private tableName = "Pool"

    [<Literal>]
    let private poolKeyPrefix = "POOL"

    [<Literal>]
    let private gamblerKeyPrefix = "GAMBLER"

    let build (poolId: Ulid) (gamblerId: Ulid) (position: int) =
        let key =
            dict
                [ "pk", AttributeValue(S = $"{poolKeyPrefix}#{poolId}")
                  "sk", AttributeValue(S = $"{gamblerKeyPrefix}#{gamblerId}") ]

        let updateExpression = "SET #currentPosition = :currentPosition"

        let attributeNames = dict [ "#currentPosition", "currentPosition" ]

        let attributeValues =
            dict [ ":currentPosition", AttributeValue(N = position.ToString()) ]

        UpdateItemRequest(
            TableName = tableName,
            Key = Dictionary key,
            UpdateExpression = updateExpression,
            ExpressionAttributeNames = Dictionary attributeNames,
            ExpressionAttributeValues = Dictionary attributeValues
        )
