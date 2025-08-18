namespace Felipearpa.Tyche.PoolLayout.Infrastructure

#nowarn "3536"

open Amazon.DynamoDBv2
open Felipearpa.Core.Paging
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.PoolLayout.Domain
open Felipearpa.Tyche.PoolLayout.Domain.PoolLayoutDictionaryTransformer
open Felipearpa.Tyche.PoolLayout.Domain.PoolLayoutMatchDictionaryTransformer

type PoolLayoutDynamoDbRepository(keySerializer: IKeySerializer, client: IAmazonDynamoDB) =
    interface IPoolLayoutRepository with
        member this.GetOpenPoolLayoutsAsync(maybeNext) =
            async {
                let! response =
                    GetOpenPoolLayoutRequestBuilder.build (maybeNext |> Option.map keySerializer.Deserialize)
                    |> client.QueryAsync
                    |> Async.AwaitTask

                let maybeLastEvaluatedKey = response.LastEvaluatedKey |> Option.ofObj

                return
                    { CursorPage.Items = response.Items |> Seq.map toPoolLayout
                      Next =
                        match maybeLastEvaluatedKey with
                        | Some lastEvaluatedKey -> keySerializer.Serialize(lastEvaluatedKey) |> Some
                        | None -> None }
            }

        member this.GetPendingMatches(id, layoutVersion, maybeNext) =
            async {
                let! response =
                    GetPendingPoolLayoutMatchesRequestBuilder.build
                        id
                        layoutVersion
                        (maybeNext |> Option.map keySerializer.Deserialize)
                    |> client.QueryAsync
                    |> Async.AwaitTask

                let maybeLastEvaluatedKey = response.LastEvaluatedKey |> Option.ofObj

                return
                    { CursorPage.Items = response.Items |> Seq.map toPoolLayoutMatch
                      Next =
                        match maybeLastEvaluatedKey with
                        | Some lastEvaluatedKey -> keySerializer.Serialize(lastEvaluatedKey) |> Some
                        | None -> None }
            }
