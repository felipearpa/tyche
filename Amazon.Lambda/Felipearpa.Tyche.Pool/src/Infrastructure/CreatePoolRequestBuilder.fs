namespace Felipearpa.Tyche.Pool.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

module CreatePoolRequestBuilder =

    let build (createPoolInput: ResolvedCreatePoolInput) =
        let poolKey =
            KeyPrefix.build PoolTable.Prefix.pool (createPoolInput.PoolId |> Ulid.value)

        let mutable attributeValues =
            dict
                [ Key.pk, AttributeValue(S = poolKey)
                  Key.sk, AttributeValue(S = poolKey)
                  PoolTable.Attribute.poolId, AttributeValue(S = (createPoolInput.PoolId |> Ulid.value))
                  PoolTable.Attribute.poolName,
                  AttributeValue(S = (createPoolInput.PoolName |> NonEmptyString100.value))
                  PoolTable.Attribute.filter, AttributeValue(S = (createPoolInput.PoolName |> NonEmptyString100.value))
                  PoolTable.Attribute.poolLayoutId, AttributeValue(S = (createPoolInput.PoolLayoutId |> Ulid.value)) ]

        Put(TableName = PoolTable.name, Item = (attributeValues |> Dictionary))
