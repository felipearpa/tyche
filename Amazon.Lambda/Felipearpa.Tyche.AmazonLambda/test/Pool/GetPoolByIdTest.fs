namespace Felipearpa.Tyche.AmazonLambda.Pool.Tests

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

module GetPoolByIdTest =
    let private ``given an existing pool`` () =
        let expectedPool: PoolResponse =
            { PoolId = "01K0DCFFB08W35AW5Q6F82R6NQ"
              PoolName = "Hello world" }

        let client = Mock<IAmazonDynamoDB>()

        let item =
            dict
                [ "pk", AttributeValue(S = "POOL#01K0DCFFB08W35AW5Q6F82R6NQ")
                  "sk", AttributeValue(S = "POOL#01K0DCFFB08W35AW5Q6F82R6NQ")
                  "filter", AttributeValue(S = "Hello world")
                  "poolId", AttributeValue(S = "01K0DCFFB08W35AW5Q6F82R6NQ")
                  "poolLayoutId", AttributeValue(S = "01HY8V7VXGPNN8CS5QY8AVMZ2C")
                  "poolName", AttributeValue(S = "Hello world") ]

        let items = [ item ]

        client
            .Setup(_.QueryAsync(It.IsAny<QueryRequest>()))
            .ReturnsAsync(QueryResponse(Items = (items |> List.map (fun it -> Dictionary it) |> ResizeArray)))
        |> ignore

        let functions =
            PoolFunctionWrapper(fun services -> services.AddSingleton<IAmazonDynamoDB>(client.Object) |> ignore)

        let context = TestLambdaContext()

        let request = APIGatewayHttpApiV2ProxyRequest()
        request.PathParameters <- dict [ "poolId", "01K0DCFFB08W35AW5Q6F82R6NQ" ]

        (expectedPool, context, request, functions)

    let private ``when requesting its information`` (functions: PoolFunctionWrapper) request context =
        async { return! functions.GetPoolByIdAsync(request, context) |> Async.AwaitTask }

    let private ``then the pool is returned`` (response: APIGatewayHttpApiV2ProxyResponse) expectedPool =
        response.StatusCode |> should equal (int HttpStatusCode.OK)

        let serializer = JsonSerializer() :> ISerializer
        let actualPool = serializer.Deserialize<PoolResponse>(response.Body)

        actualPool |> should equal expectedPool

    let private ``given a non existing pool`` () =
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
        request.PathParameters <- dict [ "poolId", "01K0DCFFB08W35AW5Q6F82R6NQ" ]

        (context, request, functions)

    let private ``then a not found response is returned`` (response: APIGatewayHttpApiV2ProxyResponse) =
        response.StatusCode |> shouldEqual (int HttpStatusCode.NotFound)

    let private ``given a request without poolId`` () =
        let functions = PoolFunctionWrapper()

        let context = TestLambdaContext()

        let request = APIGatewayHttpApiV2ProxyRequest()

        (context, request, functions)

    let private ``then a bad response is returned`` (response: APIGatewayHttpApiV2ProxyResponse) =
        response.StatusCode |> shouldEqual (int HttpStatusCode.BadRequest)

    [<Fact>]
    let ``given an existing poolId when requesting its information then the pool data is returned`` () =
        async {
            let expectedPool, context, request, functions = ``given an existing pool`` ()
            let! response = ``when requesting its information`` functions request context
            ``then the pool is returned`` response expectedPool
        }

    [<Fact>]
    let ``given a non existing poolId when requesting its information then a not found response is returned`` () =
        async {
            let context, request, functions = ``given a non existing pool`` ()
            let! response = ``when requesting its information`` functions request context
            ``then a not found response is returned`` response
        }

    [<Fact>]
    let ``given a request without poolId when requesting its information then a bad response is returned`` () =
        async {
            let context, request, functions = ``given a request without poolId`` ()
            let! response = ``when requesting its information`` functions request context
            ``then a bad response is returned`` response
        }
