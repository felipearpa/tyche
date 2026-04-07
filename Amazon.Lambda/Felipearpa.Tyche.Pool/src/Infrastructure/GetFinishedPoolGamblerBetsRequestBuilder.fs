namespace Felipearpa.Tyche.Pool.Infrastructure

open System
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

        let defaultFilterConditionExpression =
            $":now >= {ExpressionAttribute.name PoolTable.Attribute.matchDateTime}"

        let defaultAttributeValues =
            dict
                [ ":pk",
                  AttributeValue(
                      $"{KeyPrefix.build PoolTable.Prefix.gambler gamblerId.Value}#{KeyPrefix.build PoolTable.Prefix.pool poolId.Value}"
                  )
                  ":now", AttributeValue(DateTime.Now.ToUniversalTime().ToString("o")) ]

        let defaultAttributeNames =
            ExpressionAttribute.names [ Key.pk; PoolTable.Attribute.matchDateTime ]

        let filterExpression, attributeValues, attributeNames =
            match maybeSearchText with
            | None -> (defaultFilterConditionExpression, defaultAttributeValues, defaultAttributeNames)
            | Some filterText ->
                ($"{defaultFilterConditionExpression} and contains({ExpressionAttribute.name PoolTable.Attribute.filter}, :{PoolTable.Attribute.filter})",
                 defaultAttributeValues
                 |> Dictionary.union (dict [ $":{PoolTable.Attribute.filter}", AttributeValue(filterText.ToLower()) ])
                 :> IDictionary<_, _>,
                 defaultAttributeNames
                 |> Dictionary.union (ExpressionAttribute.names [ PoolTable.Attribute.filter ] |> Dictionary)
                 :> IDictionary<_, _>)

        QueryRequest(
            TableName = PoolTable.name,
            IndexName = PoolTable.Index.pendingPoolGamblerBets,
            KeyConditionExpression = keyConditionExpression,
            FilterExpression = filterExpression,
            ExpressionAttributeNames = Dictionary attributeNames,
            ExpressionAttributeValues = Dictionary attributeValues,
            ExclusiveStartKey =
                match maybeNext with
                | None -> null
                | Some next -> Dictionary(next |> deserialize)
        )
