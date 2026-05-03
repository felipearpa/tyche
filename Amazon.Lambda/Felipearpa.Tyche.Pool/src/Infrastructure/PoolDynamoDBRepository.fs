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
                          PoolName = createPoolInput.PoolName }
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
                          PoolLayoutId = joinPoolInput.PoolLayoutId
                          PoolLayoutVersion = joinPoolInput.PoolLayoutVersion }

                let incrementRequest = IncrementGamblerCountRequestBuilder.build joinPoolInput.PoolId

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
