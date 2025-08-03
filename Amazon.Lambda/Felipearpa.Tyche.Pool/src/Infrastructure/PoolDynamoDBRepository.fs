namespace Felipearpa.Tyche.Pool.Infrastructure

#nowarn "3536"

open System
open System.Collections.Generic
open System.Linq
open Amazon.DynamoDBv2
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.Account.Infrastructure
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Domain.PoolDictionaryTransformer
open Felipearpa.Type

type PoolDynamoDbRepository(keySerializer: IKeySerializer, client: IAmazonDynamoDB) =
    [<Literal>]
    let tableName = "Pool"

    [<Literal>]
    let poolText = "POOL"

    let createPoolInDbAsync createUserTransaction =
        async {
            try
                let! _ = client.TransactWriteItemsAsync(createUserTransaction) |> Async.AwaitTask
                return () |> Ok
            with
            | :? AggregateException as error when (error.InnerException :? TransactionCanceledException) ->
                return () |> Ok
            | _ -> return () |> Error
        }

    interface IPoolRepository with
        member this.GetPoolById(id) =
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
                    | Error _ -> () |> Error
            }

        member this.CreatePool(createPoolInput) =
            async {
                let createPoolRequest = CreatePoolRequestBuilder.build createPoolInput
                let createPoolGamblerRequest = JoinPoolGamblerRequestBuilder.build createPoolInput

                let requests =
                    [ TransactWriteItem(Put = createPoolRequest)
                      TransactWriteItem(Put = createPoolGamblerRequest) ]

                let createUserTransaction =
                    TransactWriteItemsRequest(TransactItems = (requests |> List))

                let! response = createPoolInDbAsync createUserTransaction

                return
                    match response with
                    | Ok _ ->
                        { CreatePoolOutput.PoolId = createPoolInput.PoolId
                          PoolName = createPoolInput.PoolName }
                        |> Ok
                    | Error _ -> () |> Error
            }

        member this.JoinPool(joinPoolInput) =
            async {
                let putRequest =
                    JoinPoolGamblerRequestBuilder.build
                        { ResolvedCreatePoolInput.PoolId = joinPoolInput.PoolId
                          PoolName = joinPoolInput.PoolName
                          OwnerGamblerId = joinPoolInput.GamblerId
                          OwnerGamblerUsername = joinPoolInput.GamblerUsername
                          PoolLayoutId = joinPoolInput.PoolLayoutId }

                let putItemRequest =
                    PutItemRequest(
                        TableName = putRequest.TableName,
                        Item = putRequest.Item,
                        ConditionExpression = putRequest.ConditionExpression
                    )

                try
                    let! _ = client.PutItemAsync(putItemRequest) |> Async.AwaitTask
                    return () |> Ok
                with :? AggregateException as error when (error.InnerException :? ConditionalCheckFailedException) ->
                    return JoinPoolDomainFailure.AlreadyJoined |> Error
            }
