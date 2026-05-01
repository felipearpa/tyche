namespace Felipearpa.Tyche.Pool.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb
open Felipearpa.Type

module GetGamblersByPoolLayoutRequestBuilder =

    let build (poolLayoutId: Ulid) (maybeNext: IDictionary<string, AttributeValue> option) =
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
                | Some next -> Dictionary next
        )
