module PoolDynamoDbRepositoryJoinPoolTest

#nowarn "3536"

open System
open System.Collections.Generic
open System.Threading
open System.Threading.Tasks
open Amazon.DynamoDBv2
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Infrastructure
open Felipearpa.Type
open FsUnitTyped
open Moq
open Xunit

let private joinInput () : ResolvedJoinPoolInput =
    { PoolId = Ulid.newOf "01K1PX1TX2NM1HG851S1V0QG6A"
      PoolName = NonEmptyString100.newOf "Polla 2026"
      GamblerId = Ulid.newOf "01K1PX1TX2NM1HG851S1V0QG6C"
      GamblerUsername = NonEmptyString100.newOf "felipe@tyche.com"
      PoolLayoutId = Ulid.newOf "01K1PX1TX2NM1HG851S1V0QG6B"
      PoolLayoutVersion = 1 }

let private buildRepository (clientMock: Mock<IAmazonDynamoDB>) =
    let keySerializerMock = Mock<IKeySerializer>()
    PoolDynamoDbRepository(keySerializerMock.Object, clientMock.Object) :> IPoolRepository

[<Fact>]
let ``given a join pool input when joining then the put and the increment are sent as a single transaction`` () =
    async {
        let clientMock = Mock<IAmazonDynamoDB>()

        let mutable capturedTransaction: TransactWriteItemsRequest = null

        clientMock
            .Setup(fun client -> client.TransactWriteItemsAsync(It.IsAny<TransactWriteItemsRequest>()))
            .Callback<TransactWriteItemsRequest, CancellationToken>(fun request _ -> capturedTransaction <- request)
            .Returns(Task.FromResult(TransactWriteItemsResponse()))
        |> ignore

        let repository = buildRepository clientMock

        let! _ = repository.JoinPoolAsync(joinInput ())

        clientMock.Verify(
            (fun client -> client.TransactWriteItemsAsync(It.IsAny<TransactWriteItemsRequest>())),
            Times.Once()
        )

        capturedTransaction |> shouldNotEqual null
        capturedTransaction.TransactItems.Count |> shouldEqual 2

        capturedTransaction.TransactItems
        |> Seq.exists (fun item -> item.Put <> null)
        |> shouldEqual true

        capturedTransaction.TransactItems
        |> Seq.exists (fun item -> item.Update <> null)
        |> shouldEqual true
    }

[<Fact>]
let ``given a join pool input when joining then the put does not assign a leaderboard position`` () =
    async {
        let clientMock = Mock<IAmazonDynamoDB>()

        let mutable capturedTransaction: TransactWriteItemsRequest = null

        clientMock
            .Setup(fun client -> client.TransactWriteItemsAsync(It.IsAny<TransactWriteItemsRequest>()))
            .Callback<TransactWriteItemsRequest, CancellationToken>(fun request _ -> capturedTransaction <- request)
            .Returns(Task.FromResult(TransactWriteItemsResponse()))
        |> ignore

        let repository = buildRepository clientMock

        let! _ = repository.JoinPoolAsync(joinInput ())

        capturedTransaction |> shouldNotEqual null

        let put =
            capturedTransaction.TransactItems
            |> Seq.find (fun item -> item.Put <> null)
            |> _.Put

        put.Item.ContainsKey(PoolTable.Attribute.position) |> shouldEqual false
        put.Item.ContainsKey(PoolTable.Attribute.beforePosition) |> shouldEqual false
    }

[<Fact>]
let ``given a transaction cancelled by a conditional check when joining then AlreadyJoined is returned`` () =
    async {
        let clientMock = Mock<IAmazonDynamoDB>()

        let cancellationReasons =
            ResizeArray
                [ CancellationReason(Code = "ConditionalCheckFailed")
                  CancellationReason(Code = "None") ]

        clientMock
            .Setup(fun client -> client.TransactWriteItemsAsync(It.IsAny<TransactWriteItemsRequest>()))
            .Throws(
                AggregateException(
                    TransactionCanceledException("transaction cancelled", CancellationReasons = cancellationReasons)
                )
            )
        |> ignore

        let repository = buildRepository clientMock

        let! result = repository.JoinPoolAsync(joinInput ())

        result |> shouldEqual (Error JoinPoolDomainFailure.AlreadyJoined)
    }
