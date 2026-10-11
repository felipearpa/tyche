namespace Felipearpa.Tyche.Pool.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb
open Felipearpa.Type

module GetGamblersByPoolLayoutRequestBuilder =

    /// The start key of the gamblers-by-layout index: a membership's table keys and its
    /// layout and pool/gambler index keys.
    let startKeyContract (poolLayoutId: Ulid) : StartKeyContract =
        Map
            [ Key.pk, StringStartingWith(KeyPrefix.build PoolTable.Prefix.pool "")
              Key.sk, StringStartingWith(KeyPrefix.build PoolTable.Prefix.gambler "")
              PoolTable.Attribute.getGamblersByPoolLayoutPk,
              StringEqualTo(KeyPrefix.build PoolTable.Prefix.poolLayout poolLayoutId.Value)
              PoolTable.Attribute.getGamblersByPoolLayoutSk, StringStartingWith(KeyPrefix.build PoolTable.Prefix.pool "") ]

    let build
        (poolLayoutId: Ulid)
        (maybeNext: string option)
        (deserialize: string -> IDictionary<string, AttributeValue>)
        =
        let keyConditionExpression =
            $"{ExpressionAttribute.name PoolTable.Attribute.getGamblersByPoolLayoutPk} = :pk"

        let attributeValues =
            dict
                [ ":pk",
                  AttributeValue(S = KeyPrefix.build PoolTable.Prefix.poolLayout (poolLayoutId |> Ulid.value)) ]

        let attributeNames =
            ExpressionAttribute.names [ PoolTable.Attribute.getGamblersByPoolLayoutPk ]

        QueryRequest(
            TableName = PoolTable.name,
            IndexName = PoolTable.Index.gamblersByPoolLayout,
            KeyConditionExpression = keyConditionExpression,
            ExpressionAttributeNames = Dictionary attributeNames,
            ExpressionAttributeValues = Dictionary attributeValues,
            ExclusiveStartKey =
                match maybeNext with
                | None -> null
                | Some next -> StartKeyContract.startKey (startKeyContract poolLayoutId) deserialize next
        )
