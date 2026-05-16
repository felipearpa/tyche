module PoolDynamoDbRepositoryPropagateUsernameTest

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

let private gamblerId = Ulid.newOf "01K1PX1TX2NM1HG851S1V0QG6B"
let private poolA = "01K1PX1TX2NM1HG851S1V0QG6A"
let private poolB = "01K1PX1TX2NM1HG851S1V0QG6C"

let private scoreItems =
    ResizeArray
        [ Dictionary(
              dict
                  [ "pk", AttributeValue(S = $"POOL#{poolA}")
                    "sk", AttributeValue(S = $"GAMBLER#{gamblerId.Value}")
                    "poolName", AttributeValue(S = "Polla A") ]
          )
          Dictionary(
              dict
                  [ "pk", AttributeValue(S = $"POOL#{poolB}")
                    "sk", AttributeValue(S = $"GAMBLER#{gamblerId.Value}")
                    "poolName", AttributeValue(S = "Polla B") ]
          ) ]

let private betKeys (poolId: string) =
    ResizeArray
        [ Dictionary(
              dict
                  [ "pk", AttributeValue(S = $"GAMBLER#{gamblerId.Value}#POOL#{poolId}")
                    "sk", AttributeValue(S = "MATCH#01K1PX1TX2NM1HG851S1V0QG6X") ]
          )
          Dictionary(
              dict
                  [ "pk", AttributeValue(S = $"GAMBLER#{gamblerId.Value}#POOL#{poolId}")
                    "sk", AttributeValue(S = "MATCH#01K1PX1TX2NM1HG851S1V0QG6Y") ]
          ) ]

[<Fact>]
let ``given two pools when propagating then issues one update per score row and per bet row`` () =
    async {
        let clientMock = Mock<IAmazonDynamoDB>()

        clientMock
            .Setup(fun client ->
                client.QueryAsync(
                    It.Is<QueryRequest>(fun (request: QueryRequest) ->
                        request.IndexName = PoolTable.Index.scoresByGambler)
                ))
            .ReturnsAsync(QueryResponse(Items = scoreItems))
        |> ignore

        clientMock
            .Setup(fun client ->
                client.QueryAsync(It.Is<QueryRequest>(fun (request: QueryRequest) -> request.IndexName = null)))
            .Returns<QueryRequest, CancellationToken>(fun (request: QueryRequest) _ ->
                let pk = request.ExpressionAttributeValues[":pk"].S
                let poolId = pk.Substring(pk.IndexOf("POOL#") + "POOL#".Length)
                Task.FromResult(QueryResponse(Items = betKeys poolId)))
        |> ignore

        let updateCount = ref 0
        let scoreUpdates = ResizeArray<UpdateItemRequest>()
        let betUpdates = ResizeArray<UpdateItemRequest>()

        clientMock
            .Setup(fun client -> client.UpdateItemAsync(It.IsAny<UpdateItemRequest>()))
            .Callback<UpdateItemRequest, CancellationToken>(fun request _ ->
                System.Threading.Interlocked.Increment(updateCount) |> ignore

                lock updateCount (fun () ->
                    if request.Key[Key.sk].S.StartsWith("GAMBLER#") then
                        scoreUpdates.Add(request)
                    else
                        betUpdates.Add(request)))
            .ReturnsAsync(UpdateItemResponse())
        |> ignore

        let repository =
            PoolDynamoDbRepository(Mock<IKeySerializer>().Object, clientMock.Object) :> IPoolRepository

        do! repository.PropagateGamblerUsernameAsync(gamblerId, NonEmptyString100.newOf "newname")

        updateCount.Value |> shouldEqual 6
        scoreUpdates.Count |> shouldEqual 2
        betUpdates.Count |> shouldEqual 4

        scoreUpdates
        |> Seq.forall (fun update -> update.ExpressionAttributeValues[":gamblerUsername"].S = "newname")
        |> shouldEqual true

        betUpdates
        |> Seq.forall (fun update -> update.ExpressionAttributeValues[":gamblerUsername"].S = "newname")
        |> shouldEqual true
    }
