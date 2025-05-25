namespace Felipearpa.Tyche.Pool.Infrastructure

open System.Linq
open Amazon.DynamoDBv2
open Amazon.DynamoDBv2.DataModel
open Felipearpa.Core.Paging
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Domain.PoolGamblerScoreDictionaryTransformer

type PoolGamblerScoreDynamoDbRepository(keySerializer: IKeySerializer, client: IAmazonDynamoDB) =
    let context = new DynamoDBContext(client)

    interface IPoolGamblerScoreRepository with

        member this.GetGamblerScoresAsync(gamblerId, maybeNext) =
            async {
                let request =
                    GetGamblerScoresRequestBuilder.build gamblerId (maybeNext |> Option.map keySerializer.Deserialize)

                let! response = client.QueryAsync(request) |> Async.AwaitTask
                let maybeLastEvaluatedKey = response.LastEvaluatedKey |> Option.ofObj

                return
                    { CursorPage.Items = response.Items.Select(toPoolGamblerScore)
                      Next =
                        match maybeLastEvaluatedKey with
                        | Some lastEvaluatedKey ->
                            match lastEvaluatedKey.Count with
                            | 0 -> None
                            | _ -> keySerializer.Serialize(lastEvaluatedKey) |> Some
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
                        | Some lastEvaluatedKey ->
                            match lastEvaluatedKey.Count with
                            | 0 -> None
                            | _ -> keySerializer.Serialize(lastEvaluatedKey) |> Some
                        | None -> None }
            }

        member this.GetPoolGamblerScoreAsync(poolId, gamblerId) =
            async {
                let request = GetPoolGamblerScoreRequestBuilder.build poolId gamblerId
                let! response = client.QueryAsync(request) |> Async.AwaitTask

                return
                    (match response.Items.FirstOrDefault() |> Option.ofObj with
                     | Some item -> item.ToPoolGamblerScore() |> Some
                     | None -> None)
                    |> Ok
            }
