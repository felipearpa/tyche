namespace Felipearpa.Tyche.Pool.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb
open Felipearpa.Type

module GetPoolMembersRequestBuilder =

    /// The start key of the members-by-username index: a membership's table keys and its
    /// username.
    let startKeyContract (poolId: Ulid) : StartKeyContract =
        Map
            [ Key.pk, StringEqualTo(KeyPrefix.build PoolTable.Prefix.pool poolId.Value)
              Key.sk, StringStartingWith(KeyPrefix.build PoolTable.Prefix.gambler "")
              PoolTable.Attribute.gamblerUsername, AnyString ]

    let build
        (poolId: Ulid)
        (maybeNext: string option)
        (deserialize: string -> IDictionary<string, AttributeValue>)
        =
        let keyConditionExpression = $"{ExpressionAttribute.name Key.pk} = :pk"

        let attributeValues =
            dict [ ":pk", AttributeValue(S = KeyPrefix.build PoolTable.Prefix.pool poolId.Value) ]

        let attributeNames = ExpressionAttribute.names [ Key.pk ]

        QueryRequest(
            TableName = PoolTable.name,
            IndexName = PoolTable.Index.gamblersByUsername,
            ScanIndexForward = true,
            KeyConditionExpression = keyConditionExpression,
            ExpressionAttributeNames = Dictionary attributeNames,
            ExpressionAttributeValues = Dictionary attributeValues,
            ExclusiveStartKey =
                match maybeNext with
                | None -> null
                | Some next -> StartKeyContract.startKey (startKeyContract poolId) deserialize next
        )
