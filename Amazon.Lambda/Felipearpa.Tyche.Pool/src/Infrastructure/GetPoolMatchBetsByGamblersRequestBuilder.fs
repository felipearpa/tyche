namespace Felipearpa.Tyche.Pool.Infrastructure

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb
open Felipearpa.Type

module GetPoolMatchBetsByGamblersRequestBuilder =

    let private key (poolId: Ulid) (matchId: Ulid) (gamblerId: Ulid) =
        Dictionary(
            dict
                [ Key.pk,
                  AttributeValue(
                      S =
                          $"{KeyPrefix.build PoolTable.Prefix.gambler gamblerId.Value}#{KeyPrefix.build PoolTable.Prefix.pool poolId.Value}"
                  )
                  Key.sk, AttributeValue(S = KeyPrefix.build PoolTable.Prefix.match' matchId.Value) ]
        )

    let build (poolId: Ulid) (matchId: Ulid) (gamblerIds: Ulid seq) =
        let keysAndAttributes =
            KeysAndAttributes(Keys = ResizeArray(gamblerIds |> Seq.map (key poolId matchId)))

        BatchGetItemRequest(RequestItems = Dictionary(dict [ PoolTable.name, keysAndAttributes ]))
