namespace Felipearpa.Tyche.Pool.Infrastructure

#nowarn "3536"

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

    let getGamblerCountsAsync (poolIds: Ulid list) =
        let rec fetch (request: BatchGetItemRequest) (gamblerCounts: Map<Ulid, int>) =
            async {
                let! response = client.BatchGetItemAsync(request) |> Async.AwaitTask

                let poolRootItems =
                    match response.Responses |> Option.ofObj with
                    | Some responses ->
                        match responses.TryGetValue PoolTable.name with
                        | true, items -> items :> seq<_>
                        | _ -> Seq.empty
                    | None -> Seq.empty

                let gamblerCounts =
                    poolRootItems
                    |> Seq.choose toGamblerCountEntry
                    |> Seq.fold (fun counts (poolId, count) -> counts |> Map.add poolId count) gamblerCounts

                match response.UnprocessedKeys |> Option.ofObj with
                | Some unprocessedKeys when unprocessedKeys.Count > 0 ->
                    return! fetch (BatchGetItemRequest(RequestItems = unprocessedKeys)) gamblerCounts
                | _ -> return gamblerCounts
            }

        async {
            let! gamblerCountMaps =
                poolIds
                |> List.chunkBySize batchGetItemLimit
                |> List.map (fun poolIds -> fetch (GetGamblerCountsRequestBuilder.build poolIds) Map.empty)
                |> Async.Parallel

            return gamblerCountMaps |> Seq.fold (Map.foldBack Map.add) Map.empty
        }

    interface IPoolGamblerScoreRepository with

        member this.GetGamblerScoresAsync(gamblerId, maybeNext) =
            async {
                let request =
                    GetGamblerScoresRequestBuilder.build gamblerId (maybeNext |> Option.map keySerializer.Deserialize)

                let! response = client.QueryAsync(request) |> Async.AwaitTask
                let maybeLastEvaluatedKey = response.LastEvaluatedKey |> Option.ofObj

                let scores = response.Items |> Seq.map toPoolGamblerScore |> Seq.toList
                let! gamblerCounts = scores |> List.map _.PoolId |> List.distinct |> getGamblerCountsAsync

                return
                    { CursorPage.Items =
                        scores
                        |> Seq.map (fun score ->
                            { score with
                                GamblerCount = gamblerCounts |> Map.tryFind score.PoolId })
                      Next =
                        match maybeLastEvaluatedKey with
                        | Some lastEvaluatedKey -> keySerializer.Serialize(lastEvaluatedKey) |> Some
                        | None -> None }
            }

        member this.GetPoolScoresAsync(poolId, maybeNext) =
            async {
                let request =
                    GetPoolScoresRequestBuilder.build poolId (maybeNext |> Option.map keySerializer.Deserialize)

                let! response = client.QueryAsync(request) |> Async.AwaitTask
                let maybeLastEvaluatedKey = response.LastEvaluatedKey |> Option.ofObj

                return
                    { CursorPage.Items = response.Items.Select(toPoolGamblerScore)
                      Next =
                        match maybeLastEvaluatedKey with
                        | Some lastEvaluatedKey -> keySerializer.Serialize(lastEvaluatedKey) |> Some
                        | None -> None }
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
