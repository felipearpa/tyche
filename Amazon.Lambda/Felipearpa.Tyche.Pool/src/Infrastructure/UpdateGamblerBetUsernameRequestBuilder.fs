namespace Felipearpa.Tyche.Pool.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb
open Felipearpa.Type

module UpdateGamblerBetUsernameRequestBuilder =

    let build (key: IDictionary<string, AttributeValue>) (username: NonEmptyString100) =
        let attributeNames =
            ExpressionAttribute.names [ PoolTable.Attribute.gamblerUsername ]

        let attributeValues =
            dict [ $":{PoolTable.Attribute.gamblerUsername}", AttributeValue(S = username.Value) ]

        let updateExpression =
            $"SET {ExpressionAttribute.name PoolTable.Attribute.gamblerUsername} = :{PoolTable.Attribute.gamblerUsername}"

        UpdateItemRequest(
            TableName = PoolTable.name,
            Key = Dictionary key,
            ExpressionAttributeNames = Dictionary attributeNames,
            ExpressionAttributeValues = Dictionary attributeValues,
            UpdateExpression = updateExpression
        )
