namespace Felipearpa.Tyche.PoolLayout.Infrastructure

#nowarn "3536"

open System.Linq
open System.Collections.Generic
open Amazon.DynamoDBv2
open Amazon.DynamoDBv2.Model
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
                    GetOpenPoolLayoutRequestBuilder.build maybeNext keySerializer.Deserialize
                    |> client.QueryAsync
                    |> Async.AwaitTask

                return
                    { CursorPage.Items = response.Items |> Seq.map toPoolLayout
                      Next = KeySerializer.next keySerializer response.LastEvaluatedKey }
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
                        maybeNext
                        keySerializer.Deserialize
                    |> client.QueryAsync
                    |> Async.AwaitTask

                return
                    { CursorPage.Items = response.Items |> Seq.map toPoolLayoutMatch
                      Next = KeySerializer.next keySerializer response.LastEvaluatedKey }
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

        member this.GetMatchAsync(poolLayoutId, matchId) =
            async {
                let request =
                    GetItemRequest(
                        TableName = PoolLayoutTable.name,
                        ConsistentRead = true,
                        Key =
                            Dictionary(
                                dict
                                    [ Key.pk,
                                      AttributeValue(S = KeyPrefix.build PoolLayoutTable.Prefix.poolLayout poolLayoutId.Value)
                                      Key.sk, AttributeValue(S = KeyPrefix.build PoolLayoutTable.Prefix.match' matchId.Value) ]
                            )
                    )

                let! response = client.GetItemAsync request |> Async.AwaitTask

                return
                    if response.IsItemSet && not (isNull response.Item) && response.Item.Count > 0 then
                        Some(toPoolLayoutMatch response.Item)
                    else
                        None
            }

        member this.CorrectMatchResultAsync(poolLayoutId, matchId, homeTeamScore, awayTeamScore, expectedVersion) =
            async {
                let request =
                    CorrectMatchResultRequestBuilder.build poolLayoutId matchId homeTeamScore awayTeamScore expectedVersion

                let! outcome = client.UpdateItemAsync request |> Async.AwaitTask |> Async.Catch

                match outcome with
                | Choice1Of2 _ -> return true
                | Choice2Of2 error ->
                    match error.GetBaseException() with
                    | :? ConditionalCheckFailedException -> return false
                    | baseError -> return raise baseError
            }
