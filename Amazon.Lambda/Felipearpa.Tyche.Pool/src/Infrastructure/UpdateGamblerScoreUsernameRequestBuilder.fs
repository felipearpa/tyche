namespace Felipearpa.Tyche.Pool.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb
open Felipearpa.Type

module UpdateGamblerScoreUsernameRequestBuilder =

    let build (pk: string) (sk: string) (poolName: string) (username: NonEmptyString100) =
        let keyMap = dict [ Key.pk, AttributeValue(S = pk); Key.sk, AttributeValue(S = sk) ]

        let attributeNames =
            ExpressionAttribute.names [ PoolTable.Attribute.gamblerUsername; PoolTable.Attribute.filter ]

        let attributeValues =
            dict
                [ $":{PoolTable.Attribute.gamblerUsername}", AttributeValue(S = username.Value)
                  $":{PoolTable.Attribute.filter}", AttributeValue(S = $"{poolName} {username.Value}") ]

        let updateExpression =
            $"SET {ExpressionAttribute.name PoolTable.Attribute.gamblerUsername} = :{PoolTable.Attribute.gamblerUsername}, {ExpressionAttribute.name PoolTable.Attribute.filter} = :{PoolTable.Attribute.filter}"

        UpdateItemRequest(
            TableName = PoolTable.name,
            Key = Dictionary keyMap,
            ExpressionAttributeNames = Dictionary attributeNames,
            ExpressionAttributeValues = Dictionary attributeValues,
            UpdateExpression = updateExpression
        )
