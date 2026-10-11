namespace Felipearpa.Tyche.Pool.Infrastructure

#nowarn "3536"

open System
open System.Collections.Generic
open System.Linq
open Amazon.DynamoDBv2
open Amazon.DynamoDBv2.Model
open Felipearpa.Core
open Felipearpa.Core.Paging
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Domain.InitialPoolGamblerBetTransformer
open Felipearpa.Tyche.Pool.Infrastructure
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

                return
                    { CursorPage.Items = response.Items.Select(toPoolGamblerBet)
                      Next = KeySerializer.next keySerializer response.LastEvaluatedKey }
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

                return
                    { CursorPage.Items = response.Items.Select(toPoolGamblerBet)
                      Next = KeySerializer.next keySerializer response.LastEvaluatedKey }
            }

        member this.GetLivePoolGamblerBetsAsync(poolId, gamblerId, maybeSearchText, maybeNext) =
            async {
                let request =
                    GetLivePoolGamblerBetsRequestBuilder.build
                        poolId
                        gamblerId
                        maybeSearchText
                        maybeNext
                        keySerializer.Deserialize

                let! response = client.QueryAsync(request) |> Async.AwaitTask

                return
                    { CursorPage.Items = response.Items.Select(toPoolGamblerBet)
                      Next = KeySerializer.next keySerializer response.LastEvaluatedKey }
            }

        member this.GetPoolMatchGamblerBetsAsync(poolId, matchId, maybeNext) =
            async {
                let request =
                    GetPoolMatchGamblerBetsRequestBuilder.build
                        poolId
                        matchId
                        maybeNext
                        keySerializer.Deserialize

                let! response = client.QueryAsync(request) |> Async.AwaitTask

                return
                    { CursorPage.Items = response.Items.Select(toPoolGamblerBet)
                      Next = KeySerializer.next keySerializer response.LastEvaluatedKey }
            }

        member this.GetPoolGamblerBetByIdAsync(poolId, gamblerId, matchId) =
            async {
                let request = GetPoolGamblerBetByIdRequestBuilder.build poolId gamblerId matchId
                let! response = client.GetItemAsync(request) |> Async.AwaitTask

                return
                    if response.IsItemSet then
                        response.Item |> toPoolGamblerBet |> Some
                    else
                        None
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

        member this.MaterializeBetAsync(initialBet, binding) =
            async {
                let item = Dictionary(initialBet |> toAmazonItem)

                binding
                |> Option.iter (fun binding ->
                    for attribute, value in
                        PoolProjectionRequestBuilder.projectionAttributes
                            initialBet.PoolId
                            initialBet.GamblerId
                            initialBet.MatchId
                            binding do
                        item[attribute] <- value)

                let request =
                    PoolProjectionRequestBuilder.materialize item initialBet.PoolId initialBet.GamblerId binding

                let! outcome = client.TransactWriteItemsAsync(request) |> Async.AwaitTask |> Async.Catch

                match outcome with
                | Choice1Of2 _ -> return Materialized
                | Choice2Of2 error ->
                    match GuardedWrite.baseException error with
                    | :? TransactionCanceledException as cancelled when
                        not (isNull cancelled.CancellationReasons)
                        && cancelled.CancellationReasons.Count = 2
                        ->
                        match cancelled.CancellationReasons[0].Code, cancelled.CancellationReasons[1].Code with
                        | "ConditionalCheckFailed", _ -> return AlreadyMaterialized
                        | _, "ConditionalCheckFailed" -> return BindingChanged
                        // Contention on the membership: re-read the binding and try again.
                        | ("TransactionConflict" | "None"), ("TransactionConflict" | "None") -> return BindingChanged
                        | _ -> return raise (GuardedWrite.baseException error)
                    | baseError -> return raise baseError
            }

        member this.RaisePoolLayoutVersionAsync(poolId, gamblerId, poolLayoutVersion) =
            async {
                let request = UpdatePoolLayoutVersionRequestBuilder.build poolId gamblerId poolLayoutVersion
                let! _ = GuardedWrite.run (client.UpdateItemAsync(request) |> Async.AwaitTask |> Async.Ignore)
                return ()
            }
