namespace Felipearpa.Tyche.Pool.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

module JoinPoolGamblerRequestBuilder =

    let build (createPoolInput: ResolvedCreatePoolInput) =
        let poolLayoutId = createPoolInput.PoolLayoutId |> Ulid.value
        let poolId = createPoolInput.PoolId.Value
        let gamblerId = createPoolInput.OwnerGamblerId.Value

        let gamblersByPoolLayoutPk = KeyPrefix.build PoolTable.Prefix.poolLayout poolLayoutId

        let gamblersByPoolLayoutSk =
            $"{KeyPrefix.build PoolTable.Prefix.pool poolId}#{KeyPrefix.build PoolTable.Prefix.gambler gamblerId}"

        let attributeValues =
            dict
                [ Key.pk, AttributeValue(S = KeyPrefix.build PoolTable.Prefix.pool poolId)
                  Key.sk, AttributeValue(S = KeyPrefix.build PoolTable.Prefix.gambler gamblerId)
                  PoolTable.Attribute.poolId, AttributeValue(S = poolId)
                  PoolTable.Attribute.poolName, AttributeValue(S = createPoolInput.PoolName.Value)
                  PoolTable.Attribute.gamblerId, AttributeValue(S = gamblerId)
                  PoolTable.Attribute.gamblerUsername, AttributeValue(S = createPoolInput.OwnerGamblerUsername.Value)
                  PoolTable.Attribute.status, AttributeValue(S = "OPENED")
                  PoolTable.Attribute.filter,
                  AttributeValue(S = $"{createPoolInput.PoolName} {createPoolInput.OwnerGamblerUsername}")
                  PoolTable.Attribute.poolLayoutId, AttributeValue(S = poolLayoutId)
                  PoolTable.Attribute.score, AttributeValue(N = "0")
                  PoolTable.Attribute.poolLayoutVersion,
                  AttributeValue(N = createPoolInput.PoolLayoutVersion.ToString())
                  PoolTable.Attribute.getGamblersByPoolLayoutPk, AttributeValue(S = gamblersByPoolLayoutPk)
                  PoolTable.Attribute.getGamblersByPoolLayoutSk, AttributeValue(S = gamblersByPoolLayoutSk) ]

        Put(
            TableName = PoolTable.name,
            Item = (attributeValues |> Dictionary),
            ConditionExpression = $"attribute_not_exists({Key.pk}) AND attribute_not_exists({Key.sk})"
        )
