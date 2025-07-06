namespace Felipearpa.Tyche.Pool.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

module CreatePoolRequestBuilder =
    [<Literal>]
    let private tableName = "Pool"

    [<Literal>]
    let private poolText = "POOL"

    let build (createPoolInput: ResolvedCreatePoolInput) =
        let mutable attributeValues =
            dict
                [ "pk", AttributeValue(S = $"{poolText}#{createPoolInput.PoolId |> Ulid.value}")
                  "sk", AttributeValue(S = $"{poolText}#{createPoolInput.PoolId |> Ulid.value}")
                  "poolId", AttributeValue(S = (createPoolInput.PoolId |> Ulid.value))
                  "poolName", AttributeValue(S = (createPoolInput.PoolName |> NonEmptyString100.value))
                  "filter", AttributeValue(S = (createPoolInput.PoolName |> NonEmptyString100.value))
                  "poolLayoutId", AttributeValue(S = (createPoolInput.PoolLayoutId |> Ulid.value)) ]

        Put(TableName = tableName, Item = (attributeValues |> Dictionary))
