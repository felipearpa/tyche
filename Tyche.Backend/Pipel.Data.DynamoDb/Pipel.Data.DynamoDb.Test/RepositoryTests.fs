module RepositoryTests

open System.Collections.Generic
open System.Linq
open System.Threading.Tasks
open Amazon.DynamoDBv2
open Amazon.DynamoDBv2.Model
open Moq
open Pipel.Core
open Pipel.Data.DynamoDb
open Pipel.Data.DynamoDb.Repository
open Xunit

[<Fact>]
let ``given a basic settings when asyncFindWithCursorPagination is called then an empty page is returned`` () =
    let serializer =
        { new IKeySerializer with
            member this.Deserialize value = Dictionary<string, AttributeValue>()

            member this.Serialize dict = "" }

    let client = Mock<IAmazonDynamoDB>()

    let page = CursorPage.empty<string>

    client
        .Setup(fun it -> it.ScanAsync(It.IsAny<ScanRequest>()))
        .Returns(
            Task.FromResult(
                let response = ScanResponse()
                response.LastEvaluatedKey <- Dictionary<string, AttributeValue>()
                response.Items <- List<Dictionary<string, AttributeValue>>()
                response
            )
        )
    |> ignore

    let afterPage =
        client.Object
        |> asyncFindWithCursorPagination "table" None (fun attrs -> "") None serializer
        |> Async.RunSynchronously

    Assert.Equal(0, afterPage.Items.Count())
