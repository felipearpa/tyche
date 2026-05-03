namespace Felipearpa.Tyche.Pool.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb
open Felipearpa.Type

module UpdateMatchPositionRequestBuilder =

    let build
        (poolId: Ulid)
        (gamblerId: Ulid)
        (matchId: Ulid)
        (position: int)
        (beforePosition: int option)
        =
        let pk =
            $"{KeyPrefix.build PoolTable.Prefix.gambler gamblerId.Value}#{KeyPrefix.build PoolTable.Prefix.pool poolId.Value}"

        let sk = KeyPrefix.build PoolTable.Prefix.match' matchId.Value

        let key = dict [ Key.pk, AttributeValue(pk); Key.sk, AttributeValue(sk) ]

        let positionAssignments =
            [ $"{ExpressionAttribute.name PoolTable.Attribute.position} = :{PoolTable.Attribute.position}"
              match beforePosition with
              | Some _ ->
                  $"{ExpressionAttribute.name PoolTable.Attribute.beforePosition} = :{PoolTable.Attribute.beforePosition}"
              | None -> () ]

        let updateExpression = "SET " + System.String.Join(", ", positionAssignments)

        let conditionExpression = $"attribute_exists({Key.pk})"

        let attributeNames =
            match beforePosition with
            | Some _ ->
                ExpressionAttribute.names [ PoolTable.Attribute.position; PoolTable.Attribute.beforePosition ]
            | None -> ExpressionAttribute.names [ PoolTable.Attribute.position ]

        let attributeValues =
            dict
                [ $":{PoolTable.Attribute.position}", AttributeValue(N = position.ToString())
                  match beforePosition with
                  | Some value ->
                      $":{PoolTable.Attribute.beforePosition}", AttributeValue(N = value.ToString())
                  | None -> () ]

        UpdateItemRequest(
            TableName = PoolTable.name,
            Key = Dictionary key,
            UpdateExpression = updateExpression,
            ConditionExpression = conditionExpression,
            ExpressionAttributeNames = Dictionary attributeNames,
            ExpressionAttributeValues = Dictionary attributeValues
        )
