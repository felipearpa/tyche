namespace Felipearpa.Tyche.Pool.Infrastructure

open System
open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Core
open Felipearpa.Type

module GetPendingPoolGamblerBetsRequestBuilder =

    [<Literal>]
    let tableName = "Pool"

    [<Literal>]
    let poolKeyPrefix = "POOL"

    [<Literal>]
    let gamblerKeyPrefix = "GAMBLER"

    let build
        (poolId: Ulid)
        (gamblerId: Ulid)
        (maybeSearchText: string option)
        (maybeNext: string option)
        (deserialize: string -> IDictionary<string, AttributeValue>)
        =
        let keyConditionExpression = "#pk = :pk and #matchDateTime > :now"

        let defaultAttributeValues =
            dict
                [ ":pk", AttributeValue($"{gamblerKeyPrefix}#{gamblerId}#{poolKeyPrefix}#{poolId}")
                  ":now", AttributeValue(DateTime.Now.ToUniversalTime().ToString("o")) ]

        let defaultAttributeNames = dict [ "#pk", "pk"; "#matchDateTime", "matchDateTime" ]

        let filterExpression, attributeValues, attributeNames =
            match maybeSearchText with
            | None -> (null, defaultAttributeValues, defaultAttributeNames)
            | Some filterText ->
                ("contains(#filter, :filter)",
                 defaultAttributeValues
                 |> Dictionary.union (dict [ ":filter", AttributeValue(filterText.ToLower()) ])
                 :> IDictionary<_, _>,
                 defaultAttributeNames |> Dictionary.union (dict [ "#filter", "filter" ]) :> IDictionary<_, _>)

        QueryRequest(
            TableName = tableName,
            IndexName = "GetPendingPoolGamblerBets-index",
            KeyConditionExpression = keyConditionExpression,
            FilterExpression = filterExpression,
            ExpressionAttributeNames = Dictionary attributeNames,
            ExpressionAttributeValues = Dictionary attributeValues,
            ExclusiveStartKey =
                match maybeNext with
                | None -> null
                | Some next -> Dictionary(next |> deserialize)
        )
