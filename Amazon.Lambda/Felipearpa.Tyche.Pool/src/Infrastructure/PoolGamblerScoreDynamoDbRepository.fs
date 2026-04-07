namespace Felipearpa.Tyche.Pool.Infrastructure

#nowarn "3536"

open System.Linq
open Amazon.DynamoDBv2
open Amazon.DynamoDBv2.Model
open Felipearpa.Core
open Felipearpa.Core.Paging
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Domain.BetEvaluator
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

        member this.Compute(matchId, matchScore, onPoolScored) =
            let computedRequestId = Ulid.random().ToString()

            let computeScore (poolGamblerBet: PoolGamblerBet) =
                let scoreDelta = delta poolGamblerBet.BetScore matchScore

                let detailUpdate =
                    ComputePoolGamblerBetRequestBuilder.build poolGamblerBet matchScore computedRequestId

                let masterUpdate =
                    ComputePoolGamblerScoreRequestBuilder.build
                        poolGamblerBet.PoolId
                        poolGamblerBet.GamblerId
                        scoreDelta

                let transactRequest =
                    TransactWriteItemsRequest(
                        TransactItems =
                            ResizeArray(
                                [ TransactWriteItem(Update = detailUpdate)
                                  TransactWriteItem(Update = masterUpdate) ]
                            )
                    )

                client.TransactWriteItemsAsync transactRequest
                |> Async.AwaitTask
                |> Async.Ignore
                |> ConditionalUpdate.ignoreTransactionConflict

            let notifyIfPoolChanged (previousPoolId: Ulid option) (currentPoolId: Ulid) =
                async {
                    match previousPoolId with
                    | Some poolId when poolId <> currentPoolId -> do! onPoolScored poolId
                    | _ -> ()
                }

            let rec loop (lastPoolId: Ulid option) maybeNext =
                async {
                    let request =
                        GetBetPoolGamblerScoresByMatchRequestBuilder.build
                            matchId
                            (maybeNext |> Option.map keySerializer.Deserialize)

                    let! response = client.QueryAsync(request) |> Async.AwaitTask

                    let! lastPoolId =
                        response.Items
                        |> Seq.map toPoolGamblerBet
                        |> Seq.fold
                            (fun (state: Async<Ulid option>) bet ->
                                async {
                                    let! previousPoolId = state
                                    do! notifyIfPoolChanged previousPoolId bet.PoolId
                                    do! computeScore bet
                                    return Some bet.PoolId
                                })
                            (async { return lastPoolId })

                    match response.LastEvaluatedKey |> Option.ofObj with
                    | Some lek ->
                        let serialized = keySerializer.Serialize(lek)
                        return! loop lastPoolId (Some serialized)
                    | None -> return lastPoolId
                }

            async {
                let! lastPoolId = loop None None

                match lastPoolId with
                | Some poolId -> do! onPoolScored poolId
                | None -> ()
            }

        member this.UpdatePositions(poolId: Ulid, matchId: Ulid) =
            let rec loop (position: int) (maybeNext: string option) =
                async {
                    let request =
                        GetPoolScoresRequestBuilder.build poolId (maybeNext |> Option.map keySerializer.Deserialize)

                    let! response = client.QueryAsync(request) |> Async.AwaitTask

                    do!
                        response.Items
                        |> Seq.mapi (fun i item ->
                            let gamblerId = item[PoolTable.Attribute.gamblerId].S |> Ulid.newOf
                            let beforePosition = item[PoolTable.Attribute.currentPosition].N |> int
                            let newPosition = position + i + 1

                            let masterUpdate =
                                UpdateCurrentPositionRequestBuilder.build poolId gamblerId newPosition

                            let detailUpdate =
                                UpdateMatchPositionRequestBuilder.build
                                    poolId
                                    gamblerId
                                    matchId
                                    newPosition
                                    beforePosition

                            async {
                                do!
                                    [ client.UpdateItemAsync masterUpdate |> Async.AwaitTask |> Async.Ignore
                                      client.UpdateItemAsync detailUpdate |> Async.AwaitTask |> Async.Ignore ]
                                    |> Async.Parallel
                                    |> Async.Ignore
                            })
                        |> Seq.iterAsync id

                    match response.LastEvaluatedKey |> Option.ofObj with
                    | Some lek ->
                        let serialized = keySerializer.Serialize(lek)
                        return! loop (position + response.Items.Count) (Some serialized)
                    | None -> return ()
                }

            loop 0 None
