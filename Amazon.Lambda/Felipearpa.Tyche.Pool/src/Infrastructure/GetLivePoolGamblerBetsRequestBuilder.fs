namespace Felipearpa.Tyche.Pool.Infrastructure

open System
open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Core
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

module GetLivePoolGamblerBetsRequestBuilder =

    let private liveLowerBoundOffset = TimeSpan.FromHours 12.0

    let build
        (poolId: Ulid)
        (gamblerId: Ulid)
        (maybeSearchText: string option)
        (maybeNext: string option)
        (deserialize: string -> IDictionary<string, AttributeValue>)
        =
        let now = DateTime.UtcNow
        let lockHorizon = now + LockPolicy.lockOffset
        let lookbackFloor = now - liveLowerBoundOffset

        let keyConditionExpression =
            $"{ExpressionAttribute.name Key.pk} = :pk \
             and {ExpressionAttribute.name PoolTable.Attribute.matchDateTime} between :floor and :lockHorizon"

        let defaultFilterConditionExpression =
            $"attribute_not_exists({ExpressionAttribute.name PoolTable.Attribute.computedRequestId})"

        let defaultAttributeValues =
            dict
                [ ":pk",
                  AttributeValue(
                      $"{KeyPrefix.build PoolTable.Prefix.gambler gamblerId.Value}#{KeyPrefix.build PoolTable.Prefix.pool poolId.Value}"
                  )
                  ":floor", AttributeValue(lookbackFloor.ToString("o"))
                  ":lockHorizon", AttributeValue(lockHorizon.ToString("o")) ]

        let defaultAttributeNames =
            ExpressionAttribute.names
                [ Key.pk
                  PoolTable.Attribute.matchDateTime
                  PoolTable.Attribute.computedRequestId ]

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
            ScanIndexForward = false,
            ExclusiveStartKey =
                match maybeNext with
                | None -> null
                | Some next -> Dictionary(next |> deserialize)
        )
