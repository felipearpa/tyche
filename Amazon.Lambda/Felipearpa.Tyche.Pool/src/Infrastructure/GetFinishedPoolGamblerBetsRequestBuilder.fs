namespace Felipearpa.Tyche.Pool.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Core
open Felipearpa.Data.DynamoDb
open Felipearpa.Type

module GetFinishedPoolGamblerBetsRequestBuilder =

    let build
        (poolId: Ulid)
        (gamblerId: Ulid)
        (maybeSearchText: string option)
        (maybeNext: string option)
        (deserialize: string -> IDictionary<string, AttributeValue>)
        =
        let keyConditionExpression = $"{ExpressionAttribute.name Key.pk} = :pk"

        let defaultAttributeValues =
            dict
                [ ":pk",
                  AttributeValue(
                      $"{KeyPrefix.build PoolTable.Prefix.gambler gamblerId.Value}#{KeyPrefix.build PoolTable.Prefix.pool poolId.Value}"
                  ) ]

        let defaultAttributeNames = ExpressionAttribute.names [ Key.pk ]

        let filterExpression, attributeValues, attributeNames =
            match maybeSearchText with
            | None -> (null, defaultAttributeValues, defaultAttributeNames)
            | Some filterText ->
                ($"contains({ExpressionAttribute.name PoolTable.Attribute.filter}, :{PoolTable.Attribute.filter})",
                 defaultAttributeValues
                 |> Dictionary.union (dict [ $":{PoolTable.Attribute.filter}", AttributeValue(filterText.ToLower()) ])
                 :> IDictionary<_, _>,
                 defaultAttributeNames
                 |> Dictionary.union (ExpressionAttribute.names [ PoolTable.Attribute.filter ] |> Dictionary)
                 :> IDictionary<_, _>)

        QueryRequest(
            TableName = PoolTable.name,
            IndexName = PoolTable.Index.finishedPoolGamblerBets,
            KeyConditionExpression = keyConditionExpression,
            FilterExpression = filterExpression,
            ExpressionAttributeNames = Dictionary attributeNames,
            ExpressionAttributeValues = Dictionary attributeValues,
            ExclusiveStartKey =
                match maybeNext with
                | None -> null
                | Some next -> Dictionary(next |> deserialize)
        )
