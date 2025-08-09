namespace Felipearpa.Tyche.AmazonLambda.Tests.Pool

#nowarn "3536"

open System.Collections.Generic
open System.Net
open Amazon.DynamoDBv2
open Amazon.DynamoDBv2.Model
open Amazon.Lambda.APIGatewayEvents
open Amazon.Lambda.TestUtilities
open Felipearpa.Tyche.AmazonLambda
open Felipearpa.Tyche.Function.Response
open FsUnit.Xunit
open Microsoft.Extensions.DependencyInjection
open Moq
open Xunit

module CreatePoolTest =

    let private setupMockCreatePoolWrite (client: Mock<IAmazonDynamoDB>) =
        client
            .Setup(_.TransactWriteItemsAsync(It.IsAny<TransactWriteItemsRequest>()))
            .ReturnsAsync(TransactWriteItemsResponse())
        |> ignore

    let private setupMockGetAccountQuery (client: Mock<IAmazonDynamoDB>) =
        let items =
            [ dict
                  [ "pk", AttributeValue(S = "POOL#01K1PX1TX2NM1HG851S1V0QG6N")
                    "accountId", AttributeValue(S = "01K1PX1TX2NM1HG851S1V0QG6N")
                    "email", AttributeValue(S = "tyche@tyche.com")
                    "externalAccountId", AttributeValue(S = "01K1PX1TX2NM1HG851S1V0QG6N") ] ]

        client
            .Setup(_.QueryAsync(It.IsAny<QueryRequest>()))
            .ReturnsAsync(QueryResponse(Items = (items |> List.map (fun it -> Dictionary it) |> ResizeArray)))
        |> ignore

    let private ``given a request to create a pool`` () =
        let expectedPool =
            { PoolResponse.PoolId = "01K23DN4Q5WD5BJ5FKGTG414EG"
              PoolName = "Champions League Pool" }

        let client = Mock<IAmazonDynamoDB>()
        setupMockCreatePoolWrite client
        setupMockGetAccountQuery client

        let functions =
            PoolFunctionWrapper(fun services -> services.AddSingleton<IAmazonDynamoDB>(client.Object) |> ignore)

        let context = TestLambdaContext()

        let request = APIGatewayHttpApiV2ProxyRequest()

        request.Body <-
            """{"poolLayoutId":"01K23DN4Q5WD5BJ5FKGTG414EG","poolName":"Champions League Pool","ownerGamblerId":"01K23DN4Q5WD5BJ5FKGTG414EG"}"""

        (expectedPool, context, request, functions)

    let ``when is requested`` (functions: PoolFunctionWrapper) request context =
        async { return! functions.CreatePool(request, context) |> Async.AwaitTask }

    let ``then the pool is created`` (response: APIGatewayHttpApiV2ProxyResponse) (expectedPool: PoolResponse) =
        response.StatusCode |> should equal (int HttpStatusCode.OK)

    [<Fact>]
    let ``given a request to create a pool when is requested then the pool is created`` () =
        async {
            let expectedPool, context, request, functions =
                ``given a request to create a pool`` ()

            let! response = ``when is requested`` functions request context

            ``then the pool is created`` response expectedPool
        }
