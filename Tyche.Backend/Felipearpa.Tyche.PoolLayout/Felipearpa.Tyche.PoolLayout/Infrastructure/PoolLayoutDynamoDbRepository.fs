namespace Felipearpa.Tyche.PoolLayout.Infrastructure

open System.Linq
open Amazon.DynamoDBv2
open Felipearpa.Core.Paging
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.PoolLayout.Domain
open Felipearpa.Tyche.PoolLayout.Domain.PoolLayoutDictionaryTransformer

type PoolLayoutDynamoDbRepository(keySerializer: IKeySerializer, client: IAmazonDynamoDB) =
    interface IPoolLayoutRepository with
        member this.GetOpenedPoolLayoutsAsync(maybeNext) =
            async {
                let! response =
                    GetOpenedPoolLayoutRequestBuilder.build (maybeNext |> Option.map keySerializer.Deserialize)
                    |> client.QueryAsync
                    |> Async.AwaitTask

                let lastEvaluatedKey = response.LastEvaluatedKey

                return
                    { CursorPage.Items = response.Items.Select(toPoolLayout)
                      Next =
                        match response.LastEvaluatedKey.Count with
                        | 0 -> None
                        | _ -> keySerializer.Serialize(lastEvaluatedKey) |> Some }
            }
