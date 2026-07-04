module PoolGamblerScoreDynamoDbRepositoryGetGamblerScoresTest

#nowarn "3536"

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

let private gamblerId = Ulid.newOf "01K1PX1TX2NM1HG851S1V0QG6C"

let private scoreItem (poolId: string) =
    dict
        [ PoolTable.Attribute.poolId, AttributeValue(S = poolId)
          PoolTable.Attribute.gamblerId, AttributeValue(S = gamblerId.Value)
          PoolTable.Attribute.poolName, AttributeValue(S = "Polla 2026")
          PoolTable.Attribute.gamblerUsername, AttributeValue(S = "felipe@tyche.com") ]
    |> Dictionary

let private poolRootItem (poolId: string) (gamblerCount: int) =
    dict
        [ PoolTable.Attribute.poolId, AttributeValue(S = poolId)
          PoolTable.Attribute.gamblerCount, AttributeValue(N = string gamblerCount) ]
    |> Dictionary

let private buildRepository (clientMock: Mock<IAmazonDynamoDB>) =
    let keySerializerMock = Mock<IKeySerializer>()
    PoolGamblerScoreDynamoDbRepository(keySerializerMock.Object, clientMock.Object) :> IPoolGamblerScoreRepository

let private setupQuery (clientMock: Mock<IAmazonDynamoDB>) (items: Dictionary<string, AttributeValue> list) =
    clientMock
        .Setup(fun client -> client.QueryAsync(It.IsAny<QueryRequest>()))
        .Returns(Task.FromResult(QueryResponse(Items = ResizeArray(items))))
    |> ignore

let private setupBatchGet (clientMock: Mock<IAmazonDynamoDB>) (items: Dictionary<string, AttributeValue> list) =
    let mutable capturedRequest: BatchGetItemRequest = null

    clientMock
        .Setup(fun client -> client.BatchGetItemAsync(It.IsAny<BatchGetItemRequest>()))
        .Callback<BatchGetItemRequest, CancellationToken>(fun request _ -> capturedRequest <- request)
        .Returns(
            Task.FromResult(
                BatchGetItemResponse(
                    Responses = Dictionary(dict [ PoolTable.name, ResizeArray(items) ]),
                    UnprocessedKeys = Dictionary()
                )
            )
        )
    |> ignore

    fun () -> capturedRequest

[<Fact>]
let ``given scores in pools when getting gambler scores then each score carries its pool gambler count`` () =
    async {
        let clientMock = Mock<IAmazonDynamoDB>()

        setupQuery
            clientMock
            [ scoreItem "01K1PX1TX2NM1HG851S1V0QG6A"
              scoreItem "01K1PX1TX2NM1HG851S1V0QG6B" ]

        setupBatchGet
            clientMock
            [ poolRootItem "01K1PX1TX2NM1HG851S1V0QG6A" 3
              poolRootItem "01K1PX1TX2NM1HG851S1V0QG6B" 7 ]
        |> ignore

        let repository = buildRepository clientMock

        let! page = repository.GetGamblerScoresAsync(gamblerId, None)

        page.Items
        |> Seq.map (fun score -> score.PoolId.Value, score.GamblerCount)
        |> Seq.toList
        |> shouldEqual
            [ "01K1PX1TX2NM1HG851S1V0QG6A", Some 3
              "01K1PX1TX2NM1HG851S1V0QG6B", Some 7 ]
    }

[<Fact>]
let ``given scores when getting gambler scores then the batch get requests each pool root once`` () =
    async {
        let clientMock = Mock<IAmazonDynamoDB>()

        setupQuery
            clientMock
            [ scoreItem "01K1PX1TX2NM1HG851S1V0QG6A"
              scoreItem "01K1PX1TX2NM1HG851S1V0QG6B" ]

        let capturedRequest =
            setupBatchGet
                clientMock
                [ poolRootItem "01K1PX1TX2NM1HG851S1V0QG6A" 3
                  poolRootItem "01K1PX1TX2NM1HG851S1V0QG6B" 7 ]

        let repository = buildRepository clientMock

        let! _ = repository.GetGamblerScoresAsync(gamblerId, None)

        let keys = capturedRequest().RequestItems[PoolTable.name].Keys

        keys
        |> Seq.map (fun key -> key[Key.pk].S)
        |> Seq.toList
        |> shouldEqual [ "POOL#01K1PX1TX2NM1HG851S1V0QG6A"; "POOL#01K1PX1TX2NM1HG851S1V0QG6B" ]
    }

[<Fact>]
let ``given an empty page when getting gambler scores then no batch get is sent`` () =
    async {
        let clientMock = Mock<IAmazonDynamoDB>()

        setupQuery clientMock []

        let repository = buildRepository clientMock

        let! page = repository.GetGamblerScoresAsync(gamblerId, None)

        page.Items |> Seq.isEmpty |> shouldEqual true

        clientMock.Verify((fun client -> client.BatchGetItemAsync(It.IsAny<BatchGetItemRequest>())), Times.Never())
    }

[<Fact>]
let ``given a pool root without gambler count when getting gambler scores then the score has no gambler count`` () =
    async {
        let clientMock = Mock<IAmazonDynamoDB>()

        setupQuery clientMock [ scoreItem "01K1PX1TX2NM1HG851S1V0QG6A" ]

        setupBatchGet clientMock [] |> ignore

        let repository = buildRepository clientMock

        let! page = repository.GetGamblerScoresAsync(gamblerId, None)

        (page.Items |> Seq.exactlyOne).GamblerCount |> shouldEqual None
    }
