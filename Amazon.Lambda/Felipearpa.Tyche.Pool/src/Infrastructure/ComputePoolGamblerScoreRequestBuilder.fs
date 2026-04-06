namespace Felipearpa.Tyche.Pool.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Type

module ComputePoolGamblerScoreRequestBuilder =

    [<Literal>]
    let private tableName = "Pool"

    [<Literal>]
    let private poolKeyPrefix = "POOL"

    [<Literal>]
    let private gamblerKeyPrefix = "GAMBLER"

    let build (poolId: Ulid) (gamblerId: Ulid) (delta: int) =
        let key =
            dict
                [ "pk", AttributeValue(S = $"{poolKeyPrefix}#{poolId}")
                  "sk", AttributeValue(S = $"{gamblerKeyPrefix}#{gamblerId}") ]

        let updateExpression = "ADD #score :delta"

        let attributeNames = dict [ "#score", "score" ]

        let attributeValues = dict [ ":delta", AttributeValue(N = delta.ToString()) ]

        Update(
            TableName = tableName,
            Key = Dictionary key,
            UpdateExpression = updateExpression,
            ExpressionAttributeNames = Dictionary attributeNames,
            ExpressionAttributeValues = Dictionary attributeValues
        )
