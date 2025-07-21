namespace Felipearpa.Tyche.PoolLayout.Infrastructure

open System.Linq
open Amazon.DynamoDBv2
open Felipearpa.Core.Paging
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.PoolLayout.Domain
open Felipearpa.Tyche.PoolLayout.Domain.PoolLayoutDictionaryTransformer

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
                    { CursorPage.Items = response.Items.Select(toPoolLayout)
                      Next =
                        match maybeLastEvaluatedKey with
                        | Some lastEvaluatedKey -> keySerializer.Serialize(lastEvaluatedKey) |> Some
                        | None -> None }
            }
