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
open Felipearpa.Tyche.Pool.Domain.PoolGamblerBetDictionaryTransformer
open Felipearpa.Type

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

                let maybeLastEvaluatedKey = response.LastEvaluatedKey |> Option.ofObj

                return
                    { CursorPage.Items = response.Items.Select(toPoolGamblerBet)
                      Next =
                        match maybeLastEvaluatedKey with
                        | Some lastEvaluatedKey -> keySerializer.Serialize(lastEvaluatedKey) |> Some
                        | None -> None }
            }

        member this.GetPoolMatchGamblerBetsAsync(poolId, matchId, maybeNext) =
            // Ordered by the gambler's general pool score, so the page is driven by the
            // leaderboard (already score-ranked) and each gambler's bet is hydrated by key.
            let rec drainBatchAsync (requestItems: Dictionary<string, KeysAndAttributes>) acc =
                async {
                    let! response =
                        client.BatchGetItemAsync(BatchGetItemRequest(RequestItems = requestItems))
                        |> Async.AwaitTask

                    let acc =
                        match response.Responses |> Option.ofObj with
                        | Some responses ->
                            match responses.TryGetValue PoolTable.name with
                            | true, items -> (items |> List.ofSeq) @ acc
                            | _ -> acc
                        | None -> acc

                    match response.UnprocessedKeys |> Option.ofObj with
                    | Some unprocessed when unprocessed.Count > 0 -> return! drainBatchAsync unprocessed acc
                    | _ -> return acc
                }

            let getMatchBetsAsync (gamblerIds: Ulid list) =
                async {
                    let! chunks =
                        gamblerIds
                        |> List.chunkBySize 100
                        |> List.map (fun chunk ->
                            let request = GetPoolMatchBetsByGamblersRequestBuilder.build poolId matchId chunk
                            drainBatchAsync request.RequestItems [])
                        |> Async.Parallel

                    return chunks |> Array.toList |> List.collect id
                }

            async {
                let leaderboardRequest =
                    GetPoolScoresRequestBuilder.build poolId (maybeNext |> Option.map keySerializer.Deserialize)

                let! leaderboardResponse = client.QueryAsync(leaderboardRequest) |> Async.AwaitTask

                let rankedGamblerIds =
                    leaderboardResponse.Items
                    |> Seq.map (fun item -> item[PoolTable.Attribute.gamblerId].S |> Ulid.newOf)
                    |> List.ofSeq

                let! betItems = getMatchBetsAsync rankedGamblerIds

                let betByGamblerId =
                    betItems
                    |> List.map (toPoolGamblerBet >> fun bet -> bet.GamblerId, bet)
                    |> Map.ofList

                let items =
                    rankedGamblerIds
                    |> List.choose (fun gamblerId -> betByGamblerId |> Map.tryFind gamblerId)
                    |> List.filter _.isLocked

                return
                    { CursorPage.Items = items
                      Next =
                        match leaderboardResponse.LastEvaluatedKey |> Option.ofObj with
                        | Some lastEvaluatedKey -> keySerializer.Serialize(lastEvaluatedKey) |> Some
                        | None -> None }
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

        member this.MaterializeMatchForGamblerAsync(initialBet, newPoolLayoutVersion) =
            async {
                let item = initialBet |> toAmazonItem
                let putRequest = AddPoolGamblerMatchRequestBuilder.build item

                try
                    let! _ = client.PutItemAsync(putRequest) |> Async.AwaitTask
                    ()
                with :? AggregateException as error when (error.InnerException :? ConditionalCheckFailedException) ->
                    ()

                let updateRequest =
                    UpdatePoolLayoutVersionRequestBuilder.build initialBet.PoolId initialBet.GamblerId newPoolLayoutVersion

                try
                    let! _ = client.UpdateItemAsync(updateRequest) |> Async.AwaitTask
                    ()
                with :? AggregateException as error when (error.InnerException :? ConditionalCheckFailedException) ->
                    ()
            }

        member this.DeleteUncomputedBetsAsync(poolId, gamblerId) =
            let queryAllKeysAsync () =
                let rec loop (acc: IDictionary<string, AttributeValue> list) maybeNext =
                    async {
                        let request = GetPoolGamblerBetKeysRequestBuilder.build poolId gamblerId maybeNext
                        let! response = client.QueryAsync(request) |> Async.AwaitTask

                        let acc =
                            (response.Items |> Seq.cast<IDictionary<string, AttributeValue>> |> List.ofSeq)
                            @ acc

                        match response.LastEvaluatedKey |> Option.ofObj with
                        | Some lek when lek.Count > 0 -> return! loop acc (Some(lek :> IDictionary<_, _>))
                        | _ -> return acc
                    }

                loop [] None

            let deleteIfUncomputedAsync (key: IDictionary<string, AttributeValue>) =
                async {
                    let request =
                        DeleteItemRequest(
                            TableName = PoolTable.name,
                            Key = Dictionary key,
                            ConditionExpression = $"attribute_not_exists({PoolTable.Attribute.computedRequestId})"
                        )

                    try
                        let! _ = client.DeleteItemAsync(request) |> Async.AwaitTask
                        return ()
                    with
                    | :? ConditionalCheckFailedException -> return ()
                    | :? AggregateException as error when (error.InnerException :? ConditionalCheckFailedException) ->
                        return ()
                }

            async {
                let! keys = queryAllKeysAsync ()
                do! keys |> List.map deleteIfUncomputedAsync |> Async.Parallel |> Async.Ignore
                return ()
            }
