module RepositoryTests

open System.Collections.Generic
open System.Threading.Tasks
open Amazon.DynamoDBv2
open Amazon.DynamoDBv2.Model
open Felipearpa.Core.Paging
open Moq
open Felipearpa.Data.DynamoDb
open Felipearpa.Data.DynamoDb.Reader
open Xunit
open FsUnit.Xunit

let buildKeySerializer () =
    { new IKeySerializer with
        member this.Deserialize value = Dictionary<string, AttributeValue>()

        member this.Serialize dict = "" }

let buildClient () =
    let client = Mock<IAmazonDynamoDB>()

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

    client

[<Fact>]
let ``given an empty result when asyncFindWithCursorPagination is called then an empty page is returned`` () =
    let keySerializer = buildKeySerializer ()

    let client = buildClient ()

    let page =
        client.Object
        |> asyncScan "table" None (fun _ -> "") None keySerializer
        |> Async.RunSynchronously

    page |> CursorPage.isEmpty |> should equal true
