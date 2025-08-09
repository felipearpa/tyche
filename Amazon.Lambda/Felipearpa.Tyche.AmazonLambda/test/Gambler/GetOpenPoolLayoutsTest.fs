namespace Felipearpa.Tyche.AmazonLambda.Tests.Gambler

#nowarn "3536"

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
open FsUnit.Xunit
open FsUnitTyped
open Microsoft.Extensions.DependencyInjection
open Moq
open Xunit

module GetOpenPoolLayoutsTest =

    let ``given a request to query the open pool layouts`` () =
        let expectedPoolScores: CursorPage<PoolGamblerScoreResponse> =
            { Items =
                seq {
                    { PoolId = "01K0DCFFB08W35AW5Q6F82R6NQ"
                      PoolName = "Hello world"
                      GamblerId = "01H1CMCDHH99FH8FDY36S0YH3A"
                      GamblerUsername = "felipearcila@gmail.com"
                      CurrentPosition = Some 1
                      BeforePosition = Some 2
                      Score = Some 15 }

                    { PoolId = "01KZXZNSK2WT2BVRZBW1H7E92Y"
                      PoolName = "Hola mundo"
                      GamblerId = "01HZX6WDB4WF5D6EWFDRKHYM2P"
                      GamblerUsername = "second@example.com"
                      CurrentPosition = Some 3
                      BeforePosition = Some 4
                      Score = Some 20 }
                }
              Next = None }

        let client = Mock<IAmazonDynamoDB>()

        let items =
            [ dict
                  [ "poolId", AttributeValue(S = "01K0DCFFB08W35AW5Q6F82R6NQ")
                    "gamblerId", AttributeValue(S = "01H1CMCDHH99FH8FDY36S0YH3A")
                    "poolName", AttributeValue(S = "Hello world")
                    "gamblerUsername", AttributeValue(S = "felipearcila@gmail.com")
                    "currentPosition", AttributeValue(N = "1")
                    "beforePosition", AttributeValue(N = "2")
                    "score", AttributeValue(N = "15") ]
              dict
                  [ "poolId", AttributeValue(S = "01KZXZNSK2WT2BVRZBW1H7E92Y")
                    "gamblerId", AttributeValue(S = "01HZX6WDB4WF5D6EWFDRKHYM2P")
                    "poolName", AttributeValue(S = "Hola mundo")
                    "gamblerUsername", AttributeValue(S = "second@example.com")
                    "currentPosition", AttributeValue(N = "3")
                    "beforePosition", AttributeValue(N = "4")
                    "score", AttributeValue(N = "20") ] ]

        client
            .Setup(_.QueryAsync(It.IsAny<QueryRequest>()))
            .ReturnsAsync(QueryResponse(Items = (items |> List.map (fun it -> Dictionary it) |> ResizeArray)))
        |> ignore

        let functions =
            GamblerFunctionWrapper(fun services -> services.AddSingleton<IAmazonDynamoDB>(client.Object) |> ignore)

        let context = TestLambdaContext()

        let request = APIGatewayHttpApiV2ProxyRequest()
        request.PathParameters <- dict [ "gamblerId", "01K0DCFFB08W35AW5Q6F82R6NQ" ]

        (expectedPoolScores, context, request, functions)

    let ``when requesting its information`` (functions: GamblerFunctionWrapper) request context =
        async { return! functions.GetPoolsByGamblerId(request, context) |> Async.AwaitTask }

    let ``then the open pool layouts are returned``
        (response: APIGatewayHttpApiV2ProxyResponse)
        (expectedPoolScores: CursorPage<PoolGamblerScoreResponse>)
        =
        response.StatusCode |> should equal (int HttpStatusCode.OK)

        let serializer = JsonSerializer() :> ISerializer

        let actualPoolScores =
            serializer.Deserialize<CursorPage<PoolGamblerScoreResponse>>(response.Body)

        actualPoolScores.Items
        |> Seq.toList
        |> shouldEqual (expectedPoolScores.Items |> Seq.toList)

    let ``given a request without poolId`` () =
        let functions = GamblerFunctionWrapper()

        let context = TestLambdaContext()

        let request = APIGatewayHttpApiV2ProxyRequest()

        (context, request, functions)

    let ``then a bad request response is returned`` (response: APIGatewayHttpApiV2ProxyResponse) =
        response.StatusCode |> should equal (int HttpStatusCode.BadRequest)

    [<Fact>]
    let ``given an existing gambler when requesting its information then the associated pool scores are returned`` () =
        async {
            let expectedPoolScores, context, request, functions =
                ``given a request to query the open pool layouts`` ()

            let! response = ``when requesting its information`` functions request context

            ``then the open pool layouts are returned`` response expectedPoolScores
        }

    [<Fact>]
    let ``given a request without path parameters when requesting its information then bad request response is returned``
        ()
        =
        async {
            let context, request, functions = ``given a request without poolId`` ()
            let! response = ``when requesting its information`` functions request context
            ``then a bad request response is returned`` response
        }
