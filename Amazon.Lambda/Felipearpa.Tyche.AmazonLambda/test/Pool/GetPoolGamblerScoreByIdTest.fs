namespace Felipearpa.Tyche.AmazonLambda.Tests.Pool

#nowarn "3536"

open System.Collections.Generic
open System.Net
open Amazon.DynamoDBv2
open Amazon.DynamoDBv2.Model
open Amazon.Lambda.APIGatewayEvents
open Amazon.Lambda.TestUtilities
open Felipearpa.Core
open Felipearpa.Core.Json
open Felipearpa.Tyche.AmazonLambda
open Felipearpa.Tyche.Function.Response
open FsUnit.Xunit
open FsUnitTyped
open Microsoft.Extensions.DependencyInjection
open Moq
open Xunit

module GetPoolGamblerScoreByIdTest =

    let private ``given an existing PoolGamblerScore`` () =
        let expectedPoolGamblerScore: PoolGamblerScoreResponse =
            { PoolId = "01K0DCFFB08W35AW5Q6F82R6NQ"
              PoolName = "Hello world"
              GamblerId = "01H1CMCDHH99FH8FDY36S0YH3A"
              GamblerUsername = "felipearcila@gmail.com"
              CurrentPosition = None
              BeforePosition = None
              Score = None }

        let client = Mock<IAmazonDynamoDB>()

        let items =
            [ dict
                  [ "pk", AttributeValue(S = "POOL#01K0DCFFB08W35AW5Q6F82R6NQ")
                    "sk", AttributeValue(S = "GAMBLER#01H1CMCDHH99FH8FDY36S0YH3A")
                    "filter", AttributeValue(S = "Hello world felipearcila@gmail.com")
                    "gamblerId", AttributeValue(S = "01H1CMCDHH99FH8FDY36S0YH3A")
                    "gamblerUsername", AttributeValue(S = "felipearcila@gmail.com")
                    "getPoolGamblerScoresByGamblerSk", AttributeValue(S = "POSITION0#POOL#01K0DCFFB08W35AW5Q6F82R6NQ")
                    "poolId", AttributeValue(S = "01K0DCFFB08W35AW5Q6F82R6NQ")
                    "poolLayoutId", AttributeValue(S = "01HY8V7VXGPNN8CS5QY8AVMZ2C")
                    "poolName", AttributeValue(S = "Hello world")
                    "status", AttributeValue(S = "OPENED") ] ]

        client
            .Setup(_.QueryAsync(It.IsAny<QueryRequest>()))
            .ReturnsAsync(QueryResponse(Items = (items |> List.map (fun it -> Dictionary it) |> ResizeArray)))
        |> ignore

        let functions =
            PoolFunctionWrapper(fun services -> services.AddSingleton<IAmazonDynamoDB>(client.Object) |> ignore)

        let context = TestLambdaContext()

        let request = APIGatewayHttpApiV2ProxyRequest()

        request.PathParameters <-
            dict
                [ "poolId", "01K0DCFFB08W35AW5Q6F82R6NQ"
                  "gamblerId", "01K0DCFFB08W35AW5Q6F82R6NQ" ]

        (expectedPoolGamblerScore, context, request, functions)

    let private ``when requesting its information`` (functions: PoolFunctionWrapper) request context =
        async { return! functions.GetPoolGamblerScoreById(request, context) |> Async.AwaitTask }

    let private ``then the associated PoolGamblerScore is returned``
        (response: APIGatewayHttpApiV2ProxyResponse)
        (expectedPoolGamblerScore: PoolGamblerScoreResponse)
        =
        response.StatusCode |> should equal (int HttpStatusCode.OK)

        let serializer = JsonSerializer() :> ISerializer

        let actualPoolGamblerScore =
            serializer.Deserialize<PoolGamblerScoreResponse>(response.Body)

        actualPoolGamblerScore |> shouldEqual expectedPoolGamblerScore

    let private ``given a request without path parameters`` () =
        let functions = PoolFunctionWrapper()

        let context = TestLambdaContext()

        let request = APIGatewayHttpApiV2ProxyRequest()

        (context, request, functions)

    let private ``then a bad response is returned`` (response: APIGatewayHttpApiV2ProxyResponse) =
        response.StatusCode |> shouldEqual (int HttpStatusCode.BadRequest)

    let private ``given a non existing PoolGamblerScore`` () =
        let client = Mock<IAmazonDynamoDB>()

        let items: IDictionary<string, AttributeValue> list = []

        client
            .Setup(_.QueryAsync(It.IsAny<QueryRequest>()))
            .ReturnsAsync(QueryResponse(Items = (items |> List.map (fun it -> Dictionary it) |> ResizeArray)))
        |> ignore

        let functions =
            PoolFunctionWrapper(fun services -> services.AddSingleton<IAmazonDynamoDB>(client.Object) |> ignore)

        let context = TestLambdaContext()

        let request = APIGatewayHttpApiV2ProxyRequest()

        request.PathParameters <-
            dict
                [ "poolId", "01K0DCFFB08W35AW5Q6F82R6NQ"
                  "gamblerId", "01K0DCFFB08W35AW5Q6F82R6NQ" ]

        (context, request, functions)

    let private ``then a not found response is returned`` (response: APIGatewayHttpApiV2ProxyResponse) =
        response.StatusCode |> shouldEqual (int HttpStatusCode.NotFound)

    [<Fact>]
    let ``given an existing PoolGamblerScore when requesting its information then the associated PoolGamblerScore is returned``
        ()
        =
        async {
            let expectedPoolGamblerScore, context, request, functions =
                ``given an existing PoolGamblerScore`` ()

            let! response = ``when requesting its information`` functions request context

            ``then the associated PoolGamblerScore is returned`` response expectedPoolGamblerScore
        }

    [<Fact>]
    let ``given a non existing PoolGamblerScore when requesting its information then a not found response is returned``
        ()
        =
        async {
            let context, request, functions = ``given a non existing PoolGamblerScore`` ()

            let! response = ``when requesting its information`` functions request context

            ``then a not found response is returned`` response
        }

    [<Fact>]
    let ``given a request without path parameters when requesting its information then a bad response is returned`` () =
        async {
            let context, request, functions = ``given a request without path parameters`` ()
            let! response = ``when requesting its information`` functions request context
            ``then a bad response is returned`` response
        }
