namespace Felipearpa.Tyche.Pool.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Core
open Felipearpa.Data.DynamoDb
open Felipearpa.Type

module GetPendingPoolGamblerBetsRequestBuilder =

    /// The start key of the pending-bets index: the bet's table keys and its match date.
    /// The live-bets query reads the same index with the same contract.
    let startKeyContract (poolId: Ulid) (gamblerId: Ulid) : StartKeyContract =
        Map
            [ Key.pk,
              StringEqualTo
                  $"{KeyPrefix.build PoolTable.Prefix.gambler gamblerId.Value}#{KeyPrefix.build PoolTable.Prefix.pool poolId.Value}"
              Key.sk, StringStartingWith(KeyPrefix.build PoolTable.Prefix.match' "")
              PoolTable.Attribute.matchDateTime, AnyString ]

    let build
        (poolId: Ulid)
        (gamblerId: Ulid)
        (maybeSearchText: string option)
        (maybeNext: string option)
        (deserialize: string -> IDictionary<string, AttributeValue>)
        =
        let keyConditionExpression = $"{ExpressionAttribute.name Key.pk} = :pk"

        let defaultFilterConditionExpression =
            $"attribute_not_exists({ExpressionAttribute.name PoolTable.Attribute.computedRequestId})"

        let defaultAttributeValues =
            dict
                [ ":pk",
                  AttributeValue(
                      $"{KeyPrefix.build PoolTable.Prefix.gambler gamblerId.Value}#{KeyPrefix.build PoolTable.Prefix.pool poolId.Value}"
                  ) ]

        let defaultAttributeNames =
            ExpressionAttribute.names [ Key.pk; PoolTable.Attribute.computedRequestId ]

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
                | Some next -> StartKeyContract.startKey (startKeyContract poolId gamblerId) deserialize next
        )
