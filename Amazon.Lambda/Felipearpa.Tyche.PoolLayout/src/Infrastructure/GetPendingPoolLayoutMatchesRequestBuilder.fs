namespace Felipearpa.Tyche.PoolLayout.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb
open Felipearpa.Type

module GetPendingPoolLayoutMatchesRequestBuilder =

    /// The base-table start key of a layout's matches. A legacy pk/sk token is complete for it.
    let startKeyContract (poolLayoutId: Ulid) : StartKeyContract =
        Map
            [ Key.pk, StringEqualTo(KeyPrefix.build PoolLayoutTable.Prefix.poolLayout poolLayoutId.Value)
              Key.sk, StringStartingWith(KeyPrefix.build PoolLayoutTable.Prefix.match' "") ]

    let build
        (poolLayoutId: Ulid)
        (poolLayoutVersion: int)
        (maybeNext: string option)
        (deserialize: string -> IDictionary<string, AttributeValue>)
        =
        let keyConditionExpression =
            $"{ExpressionAttribute.name Key.pk} = :pk AND begins_with({ExpressionAttribute.name Key.sk}, :matchPrefix)"

        let filterExpression =
            $"{ExpressionAttribute.name PoolLayoutTable.Attribute.poolLayoutVersion} <= :{PoolLayoutTable.Attribute.poolLayoutVersion}"

        let attributeValues =
            dict
                [ ":pk", AttributeValue(S = KeyPrefix.build PoolLayoutTable.Prefix.poolLayout poolLayoutId.Value)
                  ":matchPrefix", AttributeValue(S = KeyPrefix.build PoolLayoutTable.Prefix.match' "")
                  $":{PoolLayoutTable.Attribute.poolLayoutVersion}", AttributeValue(N = poolLayoutVersion.ToString()) ]

        let attributeNames =
            ExpressionAttribute.names [ Key.pk; Key.sk; PoolLayoutTable.Attribute.poolLayoutVersion ]

        QueryRequest(
            TableName = PoolLayoutTable.name,
            ConsistentRead = true,
            KeyConditionExpression = keyConditionExpression,
            FilterExpression = filterExpression,
            ExpressionAttributeNames = Dictionary attributeNames,
            ExpressionAttributeValues = Dictionary attributeValues,
            ExclusiveStartKey =
                match maybeNext with
                | None -> null
                | Some next -> StartKeyContract.startKey (startKeyContract poolLayoutId) deserialize next
        )
