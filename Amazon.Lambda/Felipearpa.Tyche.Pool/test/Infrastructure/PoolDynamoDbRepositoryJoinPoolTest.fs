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
let ``given a join pool input when joining then the gambler count counter is incremented before the put`` () =
    async {
        let clientMock = Mock<IAmazonDynamoDB>()

        clientMock
            .Setup(fun client -> client.UpdateItemAsync(It.IsAny<UpdateItemRequest>()))
            .Returns(Task.FromResult(UpdateItemResponse()))
        |> ignore

        clientMock
            .Setup(fun client -> client.PutItemAsync(It.IsAny<PutItemRequest>()))
            .Returns(Task.FromResult(PutItemResponse()))
        |> ignore

        let repository = buildRepository clientMock

        let! _ = repository.JoinPoolAsync(joinInput ())

        clientMock.Verify(
            (fun client -> client.UpdateItemAsync(It.IsAny<UpdateItemRequest>())),
            Times.Once()
        )
    }

[<Fact>]
let ``given a join pool input when joining then the put does not assign a leaderboard position`` () =
    async {
        let clientMock = Mock<IAmazonDynamoDB>()

        clientMock
            .Setup(fun client -> client.UpdateItemAsync(It.IsAny<UpdateItemRequest>()))
            .Returns(Task.FromResult(UpdateItemResponse()))
        |> ignore

        let mutable capturedPut: PutItemRequest = null

        clientMock
            .Setup(fun client -> client.PutItemAsync(It.IsAny<PutItemRequest>()))
            .Callback<PutItemRequest, CancellationToken>(fun request _ -> capturedPut <- request)
            .Returns(Task.FromResult(PutItemResponse()))
        |> ignore

        let repository = buildRepository clientMock

        let! _ = repository.JoinPoolAsync(joinInput ())

        capturedPut |> shouldNotEqual null
        capturedPut.Item.ContainsKey(PoolTable.Attribute.position) |> shouldEqual false
        capturedPut.Item.ContainsKey(PoolTable.Attribute.beforePosition) |> shouldEqual false
    }

[<Fact>]
let ``given a put that fails the condition check when joining then AlreadyJoined is returned`` () =
    async {
        let clientMock = Mock<IAmazonDynamoDB>()

        clientMock
            .Setup(fun client -> client.UpdateItemAsync(It.IsAny<UpdateItemRequest>()))
            .Returns(Task.FromResult(UpdateItemResponse()))
        |> ignore

        clientMock
            .Setup(fun client -> client.PutItemAsync(It.IsAny<PutItemRequest>()))
            .Throws(AggregateException(ConditionalCheckFailedException("already joined")))
        |> ignore

        let repository = buildRepository clientMock

        let! result = repository.JoinPoolAsync(joinInput ())

        result |> shouldEqual (Error JoinPoolDomainFailure.AlreadyJoined)
    }
