namespace Felipearpa.Tyche.Pool.Infrastructure

#nowarn "3536"

open System.Collections.Generic
open System.Linq
open Amazon.DynamoDBv2
open Amazon.DynamoDBv2.Model
open Felipearpa.Core
open Felipearpa.Core.Paging
open Felipearpa.Data.DynamoDb
open Felipearpa.Data.DynamoDb.Dictionary
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Domain.PoolDictionaryTransformer
open Felipearpa.Tyche.Pool.Domain.PoolGamblerScoreDictionaryTransformer
open Felipearpa.Type

type PoolGamblerScoreDynamoDbRepository(keySerializer: IKeySerializer, client: IAmazonDynamoDB) =

    [<Literal>]
    let batchGetItemLimit = 100

    /// Pool root items read in batches, following unprocessed keys.
    let getRootItemsAsync (build: Ulid list -> BatchGetItemRequest) (poolIds: Ulid list) =
        let rec fetch (request: BatchGetItemRequest) (acc: Dictionary<string, AttributeValue> list) =
            async {
                let! response = client.BatchGetItemAsync(request) |> Async.AwaitTask

                let poolRootItems =
                    match response.Responses |> Option.ofObj with
                    | Some responses ->
                        match responses.TryGetValue PoolTable.name with
                        | true, items -> items |> List.ofSeq
                        | _ -> []
                    | None -> []

                let acc = acc @ poolRootItems

                match response.UnprocessedKeys |> Option.ofObj with
                | Some unprocessedKeys when unprocessedKeys.Count > 0 ->
                    return! fetch (BatchGetItemRequest(RequestItems = unprocessedKeys)) acc
                | _ -> return acc
            }

        async {
            let! chunks =
                poolIds
                |> List.distinct
                |> List.chunkBySize batchGetItemLimit
                |> List.map (fun chunk -> fetch (build chunk) [])
                |> Async.Parallel

            return chunks |> List.concat
        }

    let getGamblerCountsAsync (poolIds: Ulid list) =
        async {
            let! items = getRootItemsAsync GetGamblerCountsRequestBuilder.build poolIds
            return items |> List.choose toGamblerCountEntry |> Map.ofList
        }

    interface IPoolGamblerScoreRepository with

        member this.GetGamblerScoresAsync(gamblerId, maybeNext) =
            async {
                let request =
                    GetGamblerScoresRequestBuilder.build gamblerId maybeNext keySerializer.Deserialize

                let! response = client.QueryAsync(request) |> Async.AwaitTask

                let scores = response.Items |> Seq.map toPoolGamblerScore |> Seq.toList
                let! gamblerCounts = scores |> List.map _.PoolId |> List.distinct |> getGamblerCountsAsync

                return
                    { CursorPage.Items =
                        scores
                        |> Seq.map (fun score ->
                            { score with
                                GamblerCount = gamblerCounts |> Map.tryFind score.PoolId })
                      Next = KeySerializer.next keySerializer response.LastEvaluatedKey }
            }

        member this.GetPoolScoresAsync(poolId, maybeNext) =
            async {
                let request =
                    GetPoolScoresRequestBuilder.build poolId maybeNext keySerializer.Deserialize

                let! response = client.QueryAsync(request) |> Async.AwaitTask

                return
                    { CursorPage.Items = response.Items.Select(toPoolGamblerScore)
                      Next = KeySerializer.next keySerializer response.LastEvaluatedKey }
            }

        member this.GetPoolGamblerScoreByIdAsync(poolId, gamblerId) =
            async {
                let request = GetPoolGamblerScoreRequestBuilder.build poolId gamblerId
                let! response = client.QueryAsync(request) |> Async.AwaitTask

                return
                    (match response.Items.FirstOrDefault() |> Option.ofObj with
                     | Some item -> item.ToPoolGamblerScore() |> Some
                     | None -> None)
                    |> Ok
            }

        member this.GetPoolTrendsAsync(poolIds) =
            async {
                let! items = getRootItemsAsync GetPoolTrendsRequestBuilder.build poolIds
                return items |> List.map toPoolTrend
            }
