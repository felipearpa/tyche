namespace Felipearpa.Tyche.Pool.Infrastructure

#nowarn "3536"

open System.Collections.Generic
open System.Linq
open Amazon.DynamoDBv2
open Amazon.DynamoDBv2.Model
open Felipearpa.Core
open Felipearpa.Core.Paging
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Domain.PoolGamblerBetDictionaryTransformer
open Felipearpa.Tyche.Pool.Domain.PoolGamblerScoreDictionaryTransformer
open Felipearpa.Type

type PoolGamblerScoreDynamoDbRepository(keySerializer: IKeySerializer, client: IAmazonDynamoDB) =

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

        member this.Compute(matchId, matchScore) =
            async {
                let requestId = Ulid.random().ToString()
                let affectedPoolIds = HashSet<string>()

                let toUpdateAction (attr: Dictionary<string, AttributeValue>) : Async<unit> =
                    affectedPoolIds.Add(attr["poolId"].S) |> ignore

                    let updateRequest =
                        ComputePoolGamblerBetRequestBuilder.build (attr |> toPoolGamblerBet) matchScore requestId

                    client.UpdateItemAsync updateRequest |> Async.AwaitTask |> Async.Ignore

                let rec loop maybeNext =
                    async {
                        let poolGamblerScoresByMatchRequest =
                            GetBetPoolGamblerScoresByMatchRequestBuilder.build
                                matchId
                                (maybeNext |> Option.map keySerializer.Deserialize)

                        let! poolGamblerScoresByMatchResponse =
                            client.QueryAsync(poolGamblerScoresByMatchRequest) |> Async.AwaitTask

                        do!
                            poolGamblerScoresByMatchResponse.Items
                            |> Seq.map toUpdateAction
                            |> Seq.iterAsync id

                        match poolGamblerScoresByMatchResponse.LastEvaluatedKey |> Option.ofObj with
                        | Some lek ->
                            let serialized = keySerializer.Serialize(lek)
                            return! loop (Some serialized)
                        | None -> return ()
                    }

                do! loop None

                return affectedPoolIds |> Seq.map Ulid.newOf |> Set.ofSeq
            }

        member this.UpdatePositions(poolId: Ulid) =
            let rec loop (position: int) (maybeNext: string option) =
                async {
                    let request =
                        GetPoolScoresRequestBuilder.build poolId (maybeNext |> Option.map keySerializer.Deserialize)

                    let! response = client.QueryAsync(request) |> Async.AwaitTask

                    do!
                        response.Items
                        |> Seq.mapi (fun i item ->
                            let gamblerId = item["gamblerId"].S |> Ulid.newOf
                            let currentPosition = position + i + 1

                            let updateRequest =
                                UpdateCurrentPositionRequestBuilder.build poolId gamblerId currentPosition

                            client.UpdateItemAsync updateRequest |> Async.AwaitTask |> Async.Ignore)
                        |> Seq.iterAsync id

                    match response.LastEvaluatedKey |> Option.ofObj with
                    | Some lek ->
                        let serialized = keySerializer.Serialize(lek)
                        return! loop (position + response.Items.Count) (Some serialized)
                    | None -> return ()
                }

            loop 0 None
