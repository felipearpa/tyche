namespace Felipearpa.Tyche.AmazonLambda.Tests.Pool

#nowarn "3536"

open System.Collections.Generic
open System.Net
open Amazon.DynamoDBv2
open Amazon.DynamoDBv2.Model
open Amazon.Lambda.APIGatewayEvents
open Amazon.Lambda.TestUtilities
open Felipearpa.Tyche.AmazonLambda
open FsUnitTyped
open Microsoft.Extensions.DependencyInjection
open Moq
open Xunit

module JoinPoolTest =

    let private setupMockCreatePoolWrite (client: Mock<IAmazonDynamoDB>) =
        client
            .Setup(_.PutItemAsync(It.IsAny<PutItemRequest>()))
            .ReturnsAsync(PutItemResponse())
        |> ignore

    let private setupMockGetAccountQuery (client: Mock<IAmazonDynamoDB>) =
        let items =
            [ dict
                  [ "pk", AttributeValue(S = "POOL#01K1PX1TX2NM1HG851S1V0QG6N")
                    "accountId", AttributeValue(S = "01K1PX1TX2NM1HG851S1V0QG6N")
                    "email", AttributeValue(S = "tyche@tyche.com")
                    "externalAccountId", AttributeValue(S = "01K1PX1TX2NM1HG851S1V0QG6N") ] ]

        client
            .Setup(_.QueryAsync(It.Is<QueryRequest>(fun (request: QueryRequest) -> request.TableName = "Account")))
            .ReturnsAsync(QueryResponse(Items = (items |> List.map (fun it -> Dictionary it) |> ResizeArray)))
        |> ignore

    let private setupMockGetPoolQuery (client: Mock<IAmazonDynamoDB>) =
        let items =
            [ dict
                  [ "poolId", AttributeValue(S = "01K1PX1TX2NM1HG851S1V0QG6N")
                    "poolName", AttributeValue(S = "World Cup 2026")
                    "poolLayoutId", AttributeValue(S = "01K1PX1TX2NM1HG851S1V0QG6N") ] ]

        client
            .Setup(_.QueryAsync(It.Is<QueryRequest>(fun (request: QueryRequest) -> request.TableName = "Pool")))
            .ReturnsAsync(QueryResponse(Items = (items |> List.map (fun it -> Dictionary it) |> ResizeArray)))
        |> ignore

    let private ``given a request to join a pool`` () =
        let client = Mock<IAmazonDynamoDB>()
        setupMockCreatePoolWrite client
        setupMockGetAccountQuery client
        setupMockGetPoolQuery client

        let functions =
            PoolFunctionWrapper(fun services -> services.AddSingleton<IAmazonDynamoDB>(client.Object) |> ignore)

        let context = TestLambdaContext()

        let request = APIGatewayHttpApiV2ProxyRequest()

        request.PathParameters <- dict [ "poolId", "01K23DN4Q5WD5BJ5FKGTG414EG" ]
        
        // language=json
        request.Body <- """{"gamblerId":"01K23DN4Q5WD5BJ5FKGTG414EG"}"""

        (context, request, functions)

    let private ``when requested`` (functions: PoolFunctionWrapper) request context =
        async { return! functions.JoinPoolAsync(request, context) |> Async.AwaitTask }

    let private ``then the participant is added to the pool`` (response: APIGatewayHttpApiV2ProxyResponse) =
        response.StatusCode |> shouldEqual (int HttpStatusCode.NoContent)

    let private ``given a bad request to join a pool`` () =
        let client = Mock<IAmazonDynamoDB>()
        setupMockCreatePoolWrite client
        setupMockGetAccountQuery client
        setupMockGetPoolQuery client

        let functions =
            PoolFunctionWrapper(fun services -> services.AddSingleton<IAmazonDynamoDB>(client.Object) |> ignore)

        let context = TestLambdaContext()

        let request = APIGatewayHttpApiV2ProxyRequest()

        (context, request, functions)

    let private ``then a bad response is returned`` (response: APIGatewayHttpApiV2ProxyResponse) =
        response.StatusCode |> shouldEqual (int HttpStatusCode.BadRequest)

    [<Fact>]
    let ``given a request to join a pool when requested then the participant is added to the pool`` () =
        async {
            let context, request, functions = ``given a request to join a pool`` ()

            let! response = ``when requested`` functions request context

            ``then the participant is added to the pool`` response
        }

    [<Fact>]
    let ``given a bad request to join a pool when requested then a bad response is returned`` () =
        async {
            let context, request, functions = ``given a bad request to join a pool`` ()

            let! response = ``when requested`` functions request context

            ``then a bad response is returned`` response
        }
