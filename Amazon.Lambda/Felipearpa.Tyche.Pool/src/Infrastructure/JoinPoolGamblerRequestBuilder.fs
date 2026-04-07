namespace Felipearpa.Tyche.Pool.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

module JoinPoolGamblerRequestBuilder =

    [<Literal>]
    let private initialPoolLayoutVersion = 1

    let build (createPoolInput: ResolvedCreatePoolInput) =
        let mutable attributeValues =
            dict
                [ Key.pk, AttributeValue(S = KeyPrefix.build PoolTable.Prefix.pool createPoolInput.PoolId.Value)
                  Key.sk,
                  AttributeValue(S = KeyPrefix.build PoolTable.Prefix.gambler createPoolInput.OwnerGamblerId.Value)
                  PoolTable.Attribute.poolId, AttributeValue(S = createPoolInput.PoolId.Value)
                  PoolTable.Attribute.poolName, AttributeValue(S = createPoolInput.PoolName.Value)
                  PoolTable.Attribute.gamblerId, AttributeValue(S = createPoolInput.OwnerGamblerId.Value)
                  PoolTable.Attribute.gamblerUsername, AttributeValue(S = createPoolInput.OwnerGamblerUsername.Value)
                  PoolTable.Attribute.status, AttributeValue(S = "OPENED")
                  PoolTable.Attribute.filter,
                  AttributeValue(S = $"{createPoolInput.PoolName} {createPoolInput.OwnerGamblerUsername}")
                  PoolTable.Attribute.poolLayoutId, AttributeValue(S = (createPoolInput.PoolLayoutId |> Ulid.value))
                  PoolTable.Attribute.currentPosition, AttributeValue(N = "0")
                  PoolTable.Attribute.beforePosition, AttributeValue(N = "0")
                  PoolTable.Attribute.score, AttributeValue(N = "0")
                  PoolTable.Attribute.poolLayoutVersion, AttributeValue(N = initialPoolLayoutVersion.ToString()) ]

        Put(
            TableName = PoolTable.name,
            Item = (attributeValues |> Dictionary),
            ConditionExpression = $"attribute_not_exists({Key.pk}) AND attribute_not_exists({Key.sk})"
        )
