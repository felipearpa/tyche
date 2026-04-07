namespace Felipearpa.Tyche.Account.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.Pool.Infrastructure

module GetPoolByIdRequestBuilder =

    let build (id: string) =
        let keyConditionExpression =
            $"{ExpressionAttribute.name Key.pk} = :pk and {ExpressionAttribute.name Key.sk} = :sk"

        let poolKey = KeyPrefix.build PoolTable.Prefix.pool id

        let mutable attributeValues =
            dict [ ":pk", AttributeValue(S = poolKey); ":sk", AttributeValue(S = poolKey) ]

        let mutable attributeNames = ExpressionAttribute.names [ Key.pk; Key.sk ]

        QueryRequest(
            TableName = PoolTable.name,
            KeyConditionExpression = keyConditionExpression,
            ExpressionAttributeNames = Dictionary attributeNames,
            ExpressionAttributeValues = Dictionary attributeValues
        )
