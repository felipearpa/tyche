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
    let private initialPoolLayoutVersion = 1

    let build (createPoolInput: ResolvedCreatePoolInput) =
        let mutable attributeValues =
            dict
                [ "pk", AttributeValue(S = $"{poolText}#{createPoolInput.PoolId}")
                  "sk", AttributeValue(S = $"{gamblerText}#{createPoolInput.OwnerGamblerId}")
                  "poolId", AttributeValue(S = createPoolInput.PoolId.Value)
                  "poolName", AttributeValue(S = createPoolInput.PoolName.Value)
                  "gamblerId", AttributeValue(S = createPoolInput.OwnerGamblerId.Value)
                  "gamblerUsername", AttributeValue(S = createPoolInput.OwnerGamblerUsername.Value)
                  "status", AttributeValue(S = "OPENED")
                  "filter", AttributeValue(S = $"{createPoolInput.PoolName} {createPoolInput.OwnerGamblerUsername}")
                  "poolLayoutId", AttributeValue(S = (createPoolInput.PoolLayoutId |> Ulid.value))
                  "currentPosition", AttributeValue(N = "0")
                  "beforePosition", AttributeValue(N = "0")
                  "score", AttributeValue(N = "0")
                  "poolLayoutVersion", AttributeValue(N = initialPoolLayoutVersion.ToString()) ]

        Put(
            TableName = tableName,
            Item = (attributeValues |> Dictionary),
            ConditionExpression = "attribute_not_exists(pk) AND attribute_not_exists(sk)"
        )
