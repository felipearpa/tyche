namespace Felipearpa.Tyche.AmazonLambda.Tests.PoolLayout

#nowarn "3536"

open System
open System.Collections.Generic
open System.Net
open Amazon.DynamoDBv2
open Amazon.DynamoDBv2.Model
open Amazon.Lambda.APIGatewayEvents
open Amazon.Lambda.TestUtilities
open Felipearpa.Core
open Felipearpa.Core.Json
open Felipearpa.Core.Paging
open Felipearpa.Tyche.AmazonLambda
open Felipearpa.Tyche.Function.Response
open FsUnitTyped
open Microsoft.Extensions.DependencyInjection
open Moq
open Xunit

module GetOpenPoolLayoutTest =

    let private ``given a request to query the open pool layouts`` () =
        let expectedPoolLayouts: CursorPage<PoolLayoutResponse> =
            { Items =
                seq {
                    { Id = "01K0DCFFB08W35AW5Q6F82R6NQ"
                      Name = "Hello world"
                      StartDateTime = DateTime(2023, 9, 1, 12, 0, 0, DateTimeKind.Utc) }

                    { Id = "01KZXZNSK2WT2BVRZBW1H7E92Y"
                      Name = "Hola mundo"
                      StartDateTime = DateTime(2023, 10, 15, 18, 30, 0, DateTimeKind.Utc) }
                }
              Next = None }

        let client = Mock<IAmazonDynamoDB>()

        let items =
            [ dict
                  [ "id", AttributeValue(S = "01K0DCFFB08W35AW5Q6F82R6NQ")
                    "name", AttributeValue(S = "Hello world")
                    "startDateTime", AttributeValue(S = "2023-09-01T12:00:00Z") ]
              dict
                  [ "id", AttributeValue(S = "01KZXZNSK2WT2BVRZBW1H7E92Y")
                    "name", AttributeValue(S = "Hola mundo")
                    "startDateTime", AttributeValue(S = "2023-10-15T18:30:00Z") ] ]

        client
            .Setup(_.QueryAsync(It.IsAny<QueryRequest>()))
            .ReturnsAsync(QueryResponse(Items = (items |> List.map (fun it -> Dictionary it) |> ResizeArray)))
        |> ignore

        let functions =
            PoolLayoutFunctionWrapper(fun services -> services.AddSingleton<IAmazonDynamoDB>(client.Object) |> ignore)

        let context = TestLambdaContext()

        let request = APIGatewayHttpApiV2ProxyRequest()

        (expectedPoolLayouts, context, request, functions)

    let private ``when requesting`` (functions: PoolLayoutFunctionWrapper) request context =
        async { return! functions.GetOpenPoolLayouts(request, context) |> Async.AwaitTask }

    let private ``then the open pool layouts are returned``
        (response: APIGatewayHttpApiV2ProxyResponse)
        (expectedPoolLayouts: CursorPage<PoolLayoutResponse>)
        =
        response.StatusCode |> shouldEqual (int HttpStatusCode.OK)

        let serializer = JsonSerializer() :> ISerializer

        let actualPoolLayouts =
            serializer.Deserialize<CursorPage<PoolLayoutResponse>>(response.Body)

        actualPoolLayouts.Items
        |> Seq.toList
        |> shouldEqual (expectedPoolLayouts.Items |> Seq.toList)

    [<Fact>]
    let ``given a request to query the open pool layouts when requesting then the open pool layouts are returned`` () =
        async {
            let expectedPoolLayouts, context, request, functions =
                ``given a request to query the open pool layouts`` ()

            let! response = ``when requesting`` functions request context

            ``then the open pool layouts are returned`` response expectedPoolLayouts
        }
