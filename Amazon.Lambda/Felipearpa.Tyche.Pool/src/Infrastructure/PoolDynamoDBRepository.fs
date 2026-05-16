namespace Felipearpa.Tyche.Pool.Infrastructure

#nowarn "3536"

open System
open System.Collections.Generic
open System.Linq
open Amazon.DynamoDBv2
open Amazon.DynamoDBv2.Model
open Felipearpa.Core.Paging
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.Account.Infrastructure
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Domain.PoolDictionaryTransformer
open Felipearpa.Tyche.Pool.Domain.PoolLayoutGamblerDictionaryTransformer
open Felipearpa.Type

type PoolDynamoDbRepository(keySerializer: IKeySerializer, client: IAmazonDynamoDB) =
    let isAlreadyJoinedFailure (transactionException: TransactionCanceledException) =
        transactionException.CancellationReasons
        |> Seq.exists (fun reason -> reason.Code = "ConditionalCheckFailed")

    let createPoolInDbAsync createUserTransaction =
        async {
            try
                let! _ = client.TransactWriteItemsAsync(createUserTransaction) |> Async.AwaitTask
                return Ok()
            with
            | :? AggregateException as error when (error.InnerException :? TransactionCanceledException) -> return Ok()
            | _ -> return Error()
        }

    interface IPoolRepository with
        member this.GetPoolByIdAsync(id) =
            async {
                let request = GetPoolByIdRequestBuilder.build (id |> Ulid.value)

                let! response =
                    async {
                        try
                            let! response = client.QueryAsync(request) |> Async.AwaitTask
                            return response |> Ok
                        with _ ->
                            return () |> Error
                    }

                return
                    match response with
                    | Ok response ->
                        match response.Items.FirstOrDefault() |> Option.ofObj with
                        | None -> None |> Ok
                        | Some valuesMap -> valuesMap.ToPool() |> Some |> Ok
                    | Error _ -> Error()
            }

        member this.CreatePoolAsync(createPoolInput) =
            async {
                let createPoolRequest = CreatePoolRequestBuilder.build createPoolInput
                let joinPoolGamblerRequest = JoinPoolGamblerRequestBuilder.build createPoolInput

                let requests =
                    [ TransactWriteItem(Put = createPoolRequest)
                      TransactWriteItem(Put = joinPoolGamblerRequest) ]

                let createUserTransaction =
                    TransactWriteItemsRequest(TransactItems = (requests |> List))

                let! response = createPoolInDbAsync createUserTransaction

                return
                    match response with
                    | Ok _ ->
                        { CreatePoolOutput.PoolId = createPoolInput.PoolId
                          PoolName = createPoolInput.PoolName
                          CreatorGamblerId = createPoolInput.OwnerGamblerId }
                        |> Ok
                    | Error _ -> Error()
            }

        member this.JoinPoolAsync(joinPoolInput) =
            async {
                let putRequest =
                    JoinPoolGamblerRequestBuilder.build
                        { ResolvedCreatePoolInput.PoolId = joinPoolInput.PoolId
                          PoolName = joinPoolInput.PoolName
                          OwnerGamblerId = joinPoolInput.GamblerId
                          OwnerGamblerUsername = joinPoolInput.GamblerUsername
                          OwnerGamblerEmail = joinPoolInput.GamblerEmail
                          PoolLayoutId = joinPoolInput.PoolLayoutId
                          PoolLayoutVersion = joinPoolInput.PoolLayoutVersion }

                let incrementRequest =
                    IncrementGamblerCountRequestBuilder.build joinPoolInput.PoolId

                let transaction =
                    TransactWriteItemsRequest(
                        TransactItems =
                            ([ TransactWriteItem(Put = putRequest)
                               TransactWriteItem(Update = incrementRequest) ]
                             |> List)
                    )

                try
                    let! _ = client.TransactWriteItemsAsync(transaction) |> Async.AwaitTask
                    return Ok()
                with error when
                    (match error.GetBaseException() with
                     | :? TransactionCanceledException as transactionException ->
                         isAlreadyJoinedFailure transactionException
                     | _ -> false) ->
                    return JoinPoolDomainFailure.AlreadyJoined |> Error
            }

        member this.GetGamblersByPoolLayoutAsync(poolLayoutId, maybeNext) =
            async {
                let request =
                    GetGamblersByPoolLayoutRequestBuilder.build
                        poolLayoutId
                        (maybeNext |> Option.map keySerializer.Deserialize)

                let! response = client.QueryAsync(request) |> Async.AwaitTask

                let maybeLastEvaluatedKey = response.LastEvaluatedKey |> Option.ofObj

                return
                    { CursorPage.Items = response.Items |> Seq.map toPoolLayoutGambler
                      Next =
                        match maybeLastEvaluatedKey with
                        | Some lastEvaluatedKey -> keySerializer.Serialize(lastEvaluatedKey) |> Some
                        | None -> None }
            }

        member this.IsPoolMemberAsync(poolId, gamblerId) =
            async {
                let request = IsPoolMemberRequestBuilder.build poolId gamblerId

                try
                    let! response = client.GetItemAsync(request) |> Async.AwaitTask
                    return response.IsItemSet |> Ok
                with _ ->
                    return Error()
            }

        member this.PropagateGamblerUsernameAsync(gamblerId, username) =
            let maxConcurrency = 25

            let queryAllAsync buildRequest =
                let rec loop acc maybeNext =
                    async {
                        let! response = client.QueryAsync(buildRequest maybeNext: QueryRequest) |> Async.AwaitTask
                        let acc = (response.Items |> List.ofSeq) @ acc

                        match response.LastEvaluatedKey |> Option.ofObj with
                        | Some lek when lek.Count > 0 -> return! loop acc (Some(lek :> IDictionary<_, _>))
                        | _ -> return acc
                    }

                loop [] None

            let parsePoolId (pk: string) =
                let prefix = $"{PoolTable.Prefix.pool}#"
                pk.Substring(prefix.Length) |> Ulid.newOf

            let updateScoreAsync (item: IDictionary<string, AttributeValue>) =
                async {
                    let request =
                        UpdateGamblerScoreUsernameRequestBuilder.build
                            item[Key.pk].S
                            item[Key.sk].S
                            item[PoolTable.Attribute.poolName].S
                            username

                    let! _ = client.UpdateItemAsync request |> Async.AwaitTask
                    return ()
                }

            let updateBetAsync (key: IDictionary<string, AttributeValue>) =
                async {
                    let request = UpdateGamblerBetUsernameRequestBuilder.build key username
                    let! _ = client.UpdateItemAsync request |> Async.AwaitTask
                    return ()
                }

            async {
                let! scoreItems =
                    queryAllAsync (fun maybeNext -> GetGamblerScoreItemsRequestBuilder.build gamblerId maybeNext)

                let poolIds = scoreItems |> List.map (fun item -> parsePoolId item[Key.pk].S)

                let! betKeysPerPool =
                    poolIds
                    |> List.map (fun poolId ->
                        queryAllAsync (fun maybeNext ->
                            GetPoolGamblerBetKeysRequestBuilder.build poolId gamblerId maybeNext))
                    |> Async.Parallel

                let scoreUpdates = scoreItems |> List.map updateScoreAsync

                let betUpdates =
                    betKeysPerPool |> Array.toList |> List.collect (List.map updateBetAsync)

                let! _ = Async.Parallel(scoreUpdates @ betUpdates, maxDegreeOfParallelism = maxConcurrency)

                return ()
            }

        member this.DeletePoolAsync(poolId) =
            let queryAllKeysAsync buildRequest =
                let rec loop (acc: IDictionary<string, AttributeValue> list) maybeNext =
                    async {
                        let! response = client.QueryAsync(buildRequest maybeNext) |> Async.AwaitTask

                        let acc =
                            (response.Items |> Seq.cast<IDictionary<string, AttributeValue>> |> List.ofSeq)
                            @ acc

                        match response.LastEvaluatedKey |> Option.ofObj with
                        | Some lek when lek.Count > 0 -> return! loop acc (Some(lek :> IDictionary<_, _>))
                        | _ -> return acc
                    }

                loop [] None

            let deleteKeysAsync (keys: IDictionary<string, AttributeValue> seq) =
                let maxRetries = 10
                let baseDelayMs = 50

                let rec submit attempt items =
                    async {
                        match items with
                        | [] -> return ()
                        | _ when attempt >= maxRetries ->
                            return
                                failwith
                                    $"BatchWriteItem left {List.length items} unprocessed item(s) after {maxRetries} retries"
                        | _ ->
                            if attempt > 0 then
                                let delayMs = baseDelayMs * (1 <<< (attempt - 1))
                                do! Async.Sleep(Random.Shared.Next(delayMs / 2, delayMs + 1))

                            let request =
                                BatchWriteItemRequest(
                                    RequestItems = Dictionary(dict [ PoolTable.name, ResizeArray items ])
                                )

                            let! response = client.BatchWriteItemAsync(request) |> Async.AwaitTask

                            let unprocessed =
                                match response.UnprocessedItems |> Option.ofObj with
                                | Some u when u.ContainsKey(PoolTable.name) -> u[PoolTable.name] |> List.ofSeq
                                | _ -> []

                            return! submit (attempt + 1) unprocessed
                    }

                async {
                    for chunk in keys |> Seq.chunkBySize 25 do
                        let items =
                            chunk
                            |> Array.toList
                            |> List.map (fun key -> WriteRequest(DeleteRequest = DeleteRequest(Key = Dictionary key)))

                        do! submit 0 items
                }

            let parseGamblerId (key: IDictionary<string, AttributeValue>) =
                let prefix = $"{PoolTable.Prefix.gambler}#"
                key[Key.sk].S.Substring(prefix.Length) |> Ulid.newOf

            async {
                let! gamblerKeys = queryAllKeysAsync (GetPoolGamblerKeysRequestBuilder.build poolId)

                for gamblerKey in gamblerKeys do
                    let gamblerId = parseGamblerId gamblerKey
                    let! betKeys = queryAllKeysAsync (GetPoolGamblerBetKeysRequestBuilder.build poolId gamblerId)
                    do! deleteKeysAsync betKeys

                do! deleteKeysAsync gamblerKeys

                let poolPk = KeyPrefix.build PoolTable.Prefix.pool poolId.Value

                let poolKey =
                    dict [ Key.pk, AttributeValue(S = poolPk); Key.sk, AttributeValue(S = poolPk) ]

                let deletePoolRoot =
                    DeleteItemRequest(TableName = PoolTable.name, Key = Dictionary poolKey)

                let! _ = client.DeleteItemAsync(deletePoolRoot) |> Async.AwaitTask
                return ()
            }
