namespace Felipearpa.Tyche.Pool.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Type

module UpdateMatchPositionRequestBuilder =

    [<Literal>]
    let private tableName = "Pool"

    [<Literal>]
    let private matchKeyPrefix = "MATCH"

    [<Literal>]
    let private poolKeyPrefix = "POOL"

    [<Literal>]
    let private gamblerKeyPrefix = "GAMBLER"

    let build (poolId: Ulid) (gamblerId: Ulid) (matchId: Ulid) (currentPosition: int) (beforePosition: int) =
        let pk = $"{gamblerKeyPrefix}#{gamblerId}#{poolKeyPrefix}#{poolId}"

        let sk = $"{matchKeyPrefix}#{matchId}"

        let key = dict [ "pk", AttributeValue(pk); "sk", AttributeValue(sk) ]

        let updateExpression =
            "SET #currentPosition = :currentPosition, \
             #beforePosition = :beforePosition"

        let attributeNames =
            dict [ "#currentPosition", "currentPosition"; "#beforePosition", "beforePosition" ]

        let attributeValues =
            dict
                [ ":currentPosition", AttributeValue(N = currentPosition.ToString())
                  ":beforePosition", AttributeValue(N = beforePosition.ToString()) ]

        UpdateItemRequest(
            TableName = tableName,
            Key = Dictionary key,
            UpdateExpression = updateExpression,
            ExpressionAttributeNames = Dictionary attributeNames,
            ExpressionAttributeValues = Dictionary attributeValues
        )
