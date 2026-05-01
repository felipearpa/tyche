namespace Felipearpa.Tyche.PoolLayout.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb

module GetPoolLayoutByIdRequestBuilder =

    let build (id: string) =
        let keyConditionExpression =
            $"{ExpressionAttribute.name Key.pk} = :pk and {ExpressionAttribute.name Key.sk} = :sk"

        let poolLayoutKey = KeyPrefix.build PoolLayoutTable.Prefix.poolLayout id

        let attributeValues =
            dict [ ":pk", AttributeValue(S = poolLayoutKey); ":sk", AttributeValue(S = poolLayoutKey) ]

        let attributeNames = ExpressionAttribute.names [ Key.pk; Key.sk ]

        QueryRequest(
            TableName = PoolLayoutTable.name,
            KeyConditionExpression = keyConditionExpression,
            ExpressionAttributeNames = Dictionary attributeNames,
            ExpressionAttributeValues = Dictionary attributeValues
        )
