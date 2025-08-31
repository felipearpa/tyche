namespace Felipearpa.Tyche.AmazonLambda.Function.Pool.Tests

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
open Felipearpa.Tyche.AmazonLambda.Function
open Felipearpa.Tyche.Function.Response
open FsUnit.Xunit
open FsUnitTyped
open Microsoft.Extensions.DependencyInjection
open Microsoft.Extensions.Logging
open Microsoft.Extensions.Logging.Abstractions
open Moq
open Xunit

module GetGamblersByPoolIdTest =

    let private ``given an existing pending bets`` () =
        let expectedPoolScores: CursorPage<PoolGamblerScoreResponse> =
            { Items =
                seq {
                    { PoolId = "01K0DCFFB08W35AW5Q6F82R6NQ"
                      PoolName = "Hello world"
                      GamblerId = "01H1CMCDHH99FH8FDY36S0YH3A"
                      GamblerUsername = "felipearcila@gmail.com"
                      CurrentPosition = None
                      BeforePosition = None
                      Score = None }

                    { PoolId = "01KZXZNSK2WT2BVRZBW1H7E92Y"
                      PoolName = "Hola mundo"
                      GamblerId = "01HZX6WDB4WF5D6EWFDRKHYM2P"
                      GamblerUsername = "second@example.com"
                      CurrentPosition = None
                      BeforePosition = None
                      Score = None }
                }
              Next = None }

        let client = Mock<IAmazonDynamoDB>()

        let items =
            [ dict
                  [ "pk", AttributeValue(S = "POOL#01K0DCFFB08W35AW5Q6F82R6NQ")
                    "sk", AttributeValue(S = "GAMBLER#01H1CMCDHH99FH8FDY36S0YH3A")
                    "filter", AttributeValue(S = "Hello world felipearcila@gmail.com")
                    "gamblerId", AttributeValue(S = "01H1CMCDHH99FH8FDY36S0YH3A")
                    "gamblerUsername", AttributeValue(S = "felipearcila@gmail.com")
                    "getPoolGamblerScoresByGamblerSk", AttributeValue(N = "0")
                    "getPoolGamblerScoresByPoolSk", AttributeValue(N = "0")
                    "poolId", AttributeValue(S = "01K0DCFFB08W35AW5Q6F82R6NQ")
                    "poolLayoutId", AttributeValue(S = "01HY8V7VXGPNN8CS5QY8AVMZ2C")
                    "poolName", AttributeValue(S = "Hello world")
                    "status", AttributeValue(S = "OPENED") ]
              dict
                  [ "pk", AttributeValue(S = "POOL#01KZXZNSK2WT2BVRZBW1H7E92Y")
                    "sk", AttributeValue(S = "GAMBLER#01HZX6WDB4WF5D6EWFDRKHYM2P")
                    "filter", AttributeValue(S = "Hola mundo second@example.com")
                    "gamblerId", AttributeValue(S = "01HZX6WDB4WF5D6EWFDRKHYM2P")
                    "gamblerUsername", AttributeValue(S = "second@example.com")
                    "getPoolGamblerScoresByGamblerSk", AttributeValue(N = "0")
                    "getPoolGamblerScoresByPoolSk", AttributeValue(N = "0")
                    "poolId", AttributeValue(S = "01KZXZNSK2WT2BVRZBW1H7E92Y")
                    "poolLayoutId", AttributeValue(S = "01HZX7HJK63MAZK6V27ZRX50XE")
                    "poolName", AttributeValue(S = "Hola mundo")
                    "status", AttributeValue(S = "OPENED") ] ]

        client
            .Setup(_.QueryAsync(It.IsAny<QueryRequest>()))
            .ReturnsAsync(QueryResponse(Items = (items |> List.map (fun it -> Dictionary it) |> ResizeArray)))
        |> ignore

        let functions =
            PoolFunction(fun services ->
                services.AddLogging(fun builder ->
                    builder.ClearProviders() |> ignore

                    builder.AddProvider(
                        { new ILoggerProvider with
                            member _.CreateLogger _ = NullLogger.Instance
                            member _.Dispose() = () }
                    )
                    |> ignore)
                |> ignore

                services.AddSingleton<IAmazonDynamoDB>(client.Object) |> ignore)

        let context = TestLambdaContext()

        let request = APIGatewayHttpApiV2ProxyRequest()
        request.PathParameters <- dict [ "poolId", "01K0DCFFB08W35AW5Q6F82R6NQ" ]

        (expectedPoolScores, context, request, functions)

    let private ``when requesting its information`` (functions: PoolFunction) request context =
        async { return! functions.GetGamblersByPoolIdAsync(request, context) |> Async.AwaitTask }

    let private ``then associated pool scores are returned``
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

    let private ``given a request without poolId`` () =
        let functions = PoolFunction()

        let context = TestLambdaContext()

        let request = APIGatewayHttpApiV2ProxyRequest()

        (context, request, functions)

    let private ``then a bad response is returned`` (response: APIGatewayHttpApiV2ProxyResponse) =
        response.StatusCode |> shouldEqual (int HttpStatusCode.BadRequest)

    [<Fact>]
    let ``given an existing pool when requesting its information then the associated pool scores are returned`` () =
        async {
            let expectedPoolScores, context, request, functions =
                ``given an existing pending bets`` ()

            let! response = ``when requesting its information`` functions request context

            ``then associated pool scores are returned`` response expectedPoolScores
        }

    [<Fact>]
    let ``given a request without path parameters when requesting its information then bad response is returned`` () =
        async {
            let context, request, functions = ``given a request without poolId`` ()
            let! response = ``when requesting its information`` functions request context
            ``then a bad response is returned`` response
        }
