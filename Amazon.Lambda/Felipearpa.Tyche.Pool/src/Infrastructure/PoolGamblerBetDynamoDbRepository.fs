namespace Felipearpa.Tyche.Pool.Infrastructure

#nowarn "3536"

open System
open System.Linq
open Amazon.DynamoDBv2
open Amazon.DynamoDBv2.Model
open Felipearpa.Core
open Felipearpa.Core.Paging
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Domain.InitialPoolGamblerBetTransformer
open Felipearpa.Tyche.Pool.Domain.PoolGamblerBetDictionaryTransformer

type PoolGamblerBetDynamoDbRepository(keySerializer: IKeySerializer, client: IAmazonDynamoDB) =

    interface IPoolGamblerBetRepository with
        member this.GetPendingPoolGamblerBetsAsync(poolId, gamblerId, maybeSearchText, maybeNext) =
            async {
                let request =
                    GetPendingPoolGamblerBetsRequestBuilder.build
                        poolId
                        gamblerId
                        maybeSearchText
                        maybeNext
                        keySerializer.Deserialize

                let! response = client.QueryAsync(request) |> Async.AwaitTask

                let maybeLastEvaluatedKey = response.LastEvaluatedKey |> Option.ofObj

                return
                    { CursorPage.Items = response.Items.Select(toPoolGamblerBet)
                      Next =
                        match maybeLastEvaluatedKey with
                        | Some lastEvaluatedKey -> keySerializer.Serialize(lastEvaluatedKey) |> Some
                        | None -> None }
            }

        member this.GetFinishedPoolGamblerBetsAsync(poolId, gamblerId, maybeSearchText, maybeNext) =
            async {
                let request =
                    GetFinishedPoolGamblerBetsRequestBuilder.build
                        poolId
                        gamblerId
                        maybeSearchText
                        maybeNext
                        keySerializer.Deserialize

                let! response = client.QueryAsync(request) |> Async.AwaitTask

                let maybeLastEvaluatedKey = response.LastEvaluatedKey |> Option.ofObj

                return
                    { CursorPage.Items = response.Items.Select(toPoolGamblerBet)
                      Next =
                        match maybeLastEvaluatedKey with
                        | Some lastEvaluatedKey -> keySerializer.Serialize(lastEvaluatedKey) |> Some
                        | None -> None }
            }

        member this.BetAsync(poolId, gamblerId, matchId, betScore) =
            async {
                let request = BetRequestBuilder.build poolId gamblerId matchId betScore

                try
                    let! response = client.UpdateItemAsync(request) |> Async.AwaitTask
                    return response.Attributes |> toPoolGamblerBet |> Ok
                with :? AggregateException as error when (error.InnerException :? ConditionalCheckFailedException) ->
                    return Error BetFailure.MatchLocked
            }

        member this.AddPoolGamblerMatchAsync(poolGamblerBet) =
            async {
                let item = poolGamblerBet |> toAmazonItem
                let request = AddPoolGamblerMatchRequestBuilder.build item

                try
                    let! _ = client.PutItemAsync(request) |> Async.AwaitTask
                    return item |> toPoolGamblerBet |> Ok
                with :? AggregateException as error when (error.InnerException :? ConditionalCheckFailedException) ->
                    return Error AddMatchFailure.AlreadyExist
            }

        member this.AddPoolGamblerMatchesAsync(poolGamblerBets) =
            poolGamblerBets
            |> Seq.mapAsync (this :> IPoolGamblerBetRepository).AddPoolGamblerMatchAsync
