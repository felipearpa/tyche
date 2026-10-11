namespace Felipearpa.Tyche.PoolLayout.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb

module GetOpenPoolLayoutRequestBuilder =

    /// The start key of the open-layouts index: a layout root's table keys, its status, and
    /// its start date.
    let startKeyContract: StartKeyContract =
        Map
            [ Key.pk, StringStartingWith(KeyPrefix.build PoolLayoutTable.Prefix.poolLayout "")
              Key.sk, StringStartingWith(KeyPrefix.build PoolLayoutTable.Prefix.poolLayout "")
              PoolLayoutTable.Attribute.status, StringEqualTo "OPENED"
              PoolLayoutTable.Attribute.startDateTime, AnyString ]

    let build (maybeNext: string option) (deserialize: string -> IDictionary<string, AttributeValue>) =
        let keyConditionExpression =
            $"{ExpressionAttribute.name PoolLayoutTable.Attribute.status} = :status"

        let attributeValues = dict [ ":status", AttributeValue("OPENED") ]

        let attributeNames = ExpressionAttribute.names [ PoolLayoutTable.Attribute.status ]

        QueryRequest(
            TableName = PoolLayoutTable.name,
            IndexName = PoolLayoutTable.Index.getOpenedPoolLayout,
            KeyConditionExpression = keyConditionExpression,
            ExpressionAttributeNames = Dictionary attributeNames,
            ExpressionAttributeValues = Dictionary attributeValues,
            ExclusiveStartKey =
                match maybeNext with
                | None -> null
                | Some next -> StartKeyContract.startKey startKeyContract deserialize next
        )
