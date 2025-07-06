namespace Felipearpa.Tyche.Pool.Infrastructure

open System
open System.Collections.Generic
open Amazon.DynamoDBv2
open Amazon.DynamoDBv2.DataModel
open Amazon.DynamoDBv2.DocumentModel
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.Pool.Data
open Felipearpa.Tyche.Pool.Domain

type PoolDynamoDbRepository(keySerializer: IKeySerializer, client: IAmazonDynamoDB) =
    [<Literal>]
    let tableName = "Pool"

    [<Literal>]
    let poolText = "POOL"

    let context = new DynamoDBContext(client)

    let map (dictionary: IDictionary<string, AttributeValue>) =
        context.FromDocument<PoolEntity>(Document.FromAttributeMap(Dictionary(dictionary)))

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
        member this.createPool(createPoolInput) =
            async {
                let createPoolRequest = CreatePoolRequestBuilder.build createPoolInput
                let createPoolGamblerRequest = CreatePoolGamblerRequestBuilder.build createPoolInput

                let requests =
                    [ TransactWriteItem(Put = createPoolRequest)
                      TransactWriteItem(Put = createPoolGamblerRequest) ]

                let createUserTransaction =
                    TransactWriteItemsRequest(TransactItems = (requests |> List))

                let! response = createPoolInDbAsync createUserTransaction

                return
                    match response with
                    | Ok _ ->
                        { Pool.PoolId = createPoolInput.PoolId
                          PoolName = createPoolInput.PoolName }
                        |> Ok
                    | Error _ -> () |> Error
            }
