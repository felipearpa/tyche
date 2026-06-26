namespace Felipearpa.Tyche.PoolLayout.Infrastructure

#nowarn "3536"

open System.Linq
open Amazon.DynamoDBv2
open Felipearpa.Core.Paging
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.PoolLayout.Domain
open Felipearpa.Tyche.PoolLayout.Domain.PoolLayoutDictionaryTransformer
open Felipearpa.Tyche.PoolLayout.Domain.PoolLayoutMatchDictionaryTransformer
open Felipearpa.Type

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

        member this.GetPoolLayoutByIdAsync(poolLayoutId) =
            async {
                let request = GetPoolLayoutByIdRequestBuilder.build (poolLayoutId |> Ulid.value)
                let! response = client.QueryAsync(request) |> Async.AwaitTask

                return
                    match response.Items.FirstOrDefault() |> Option.ofObj with
                    | Some item -> item |> toPoolLayout |> Some
                    | None -> None
            }

        member this.GetPoolLayoutMatchesThroughVersionAsync(poolLayoutId, poolLayoutVersion, maybeNext) =
            async {
                let! response =
                    GetPendingPoolLayoutMatchesRequestBuilder.build
                        poolLayoutId
                        poolLayoutVersion
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

        member this.SetMatchFinalScoreAsync(poolLayoutId, matchId, homeTeamScore, awayTeamScore) =
            ConditionalUpdate.ignoreTransactionConflict (
                async {
                    let request =
                        SetMatchFinalScoreRequestBuilder.build poolLayoutId matchId homeTeamScore awayTeamScore

                    let! _ = client.UpdateItemAsync request |> Async.AwaitTask
                    return ()
                }
            )
