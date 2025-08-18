namespace Felipearpa.Tyche.Pool.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

module JoinPoolGamblerRequestBuilder =
    [<Literal>]
    let private tableName = "Pool"

    [<Literal>]
    let private poolText = "POOL"

    [<Literal>]
    let private gamblerText = "GAMBLER"

    [<Literal>]
    let private positionText = "POSITION"

    [<Literal>]
    let private initialPoolLayoutVersion = 1

    let build (createPoolInput: ResolvedCreatePoolInput) =
        let mutable attributeValues =
            dict
                [ "pk", AttributeValue(S = $"{poolText}#{createPoolInput.PoolId |> Ulid.value}")
                  "sk", AttributeValue(S = $"{gamblerText}#{createPoolInput.OwnerGamblerId |> Ulid.value}")
                  "poolId", AttributeValue(S = (createPoolInput.PoolId |> Ulid.value))
                  "poolName", AttributeValue(S = (createPoolInput.PoolName |> NonEmptyString100.value))
                  "gamblerId", AttributeValue(S = (createPoolInput.OwnerGamblerId |> Ulid.value))
                  "gamblerUsername",
                  AttributeValue(S = (createPoolInput.OwnerGamblerUsername |> NonEmptyString100.value))
                  "status", AttributeValue(S = "OPENED")
                  "filter",
                  AttributeValue(
                      S =
                          $"{createPoolInput.PoolName |> NonEmptyString100.value} {createPoolInput.OwnerGamblerUsername |> NonEmptyString100.value}"
                  )
                  "poolLayoutId", AttributeValue(S = (createPoolInput.PoolLayoutId |> Ulid.value))
                  "getPoolGamblerScoresByGamblerSk",
                  AttributeValue(S = $"{positionText}0#{poolText}#{createPoolInput.PoolId |> Ulid.value}")
                  "poolLayoutVersion", AttributeValue(N = initialPoolLayoutVersion.ToString()) ]

        Put(
            TableName = tableName,
            Item = (attributeValues |> Dictionary),
            ConditionExpression = "attribute_not_exists(pk) AND attribute_not_exists(sk)"
        )
