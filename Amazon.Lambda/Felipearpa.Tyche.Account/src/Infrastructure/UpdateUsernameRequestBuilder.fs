namespace Felipearpa.Tyche.Account.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb
open Felipearpa.Type

module UpdateUsernameRequestBuilder =

    let build (accountId: Ulid) (username: NonEmptyString100) =
        let keyMap =
            dict [ Key.pk, AttributeValue(S = KeyPrefix.build AccountTable.Prefix.account accountId.Value) ]

        let attributeNames = ExpressionAttribute.names [ AccountTable.Attribute.username ]

        let attributeValues =
            dict [ $":{AccountTable.Attribute.username}", AttributeValue(S = username.Value) ]

        let updateExpression =
            $"SET {ExpressionAttribute.name AccountTable.Attribute.username} = :{AccountTable.Attribute.username}"

        UpdateItemRequest(
            TableName = AccountTable.name,
            Key = Dictionary keyMap,
            ExpressionAttributeNames = Dictionary attributeNames,
            ExpressionAttributeValues = Dictionary attributeValues,
            UpdateExpression = updateExpression
        )
