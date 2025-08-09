namespace Felipearpa.Tyche.AmazonLambda.Tests.Account

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

module LinkAccountTest =

    let private setupMockLinkAccountWhenExistWrite (client: Mock<IAmazonDynamoDB>) =
        client
            .Setup(_.UpdateItemAsync(It.IsAny<UpdateItemRequest>()))
            .ReturnsAsync(UpdateItemResponse())
        |> ignore

    let private setupMockLinkAccountWhenNotExistWrite (client: Mock<IAmazonDynamoDB>) =
        client
            .Setup(_.TransactWriteItemsAsync(It.IsAny<TransactWriteItemsRequest>()))
            .ReturnsAsync(TransactWriteItemsResponse())
        |> ignore

    let private setupMockGetExistingAccountQuery (client: Mock<IAmazonDynamoDB>) =
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

    let private setupMockGetNonExistingAccountQuery (client: Mock<IAmazonDynamoDB>) =
        let items: IDictionary<string, AttributeValue> list = []

        client
            .Setup(_.QueryAsync(It.Is<QueryRequest>(fun (request: QueryRequest) -> request.TableName = "Account")))
            .ReturnsAsync(QueryResponse(Items = (items |> List.map (fun it -> Dictionary it) |> ResizeArray)))
        |> ignore

    let private ``given a request to link an existing account`` () =
        let client = Mock<IAmazonDynamoDB>()
        setupMockLinkAccountWhenExistWrite client
        setupMockGetExistingAccountQuery client

        let functions =
            AccountFunctionWrapper(fun services -> services.AddSingleton<IAmazonDynamoDB>(client.Object) |> ignore)

        let context = TestLambdaContext()

        let request = APIGatewayHttpApiV2ProxyRequest()

        request.Body <- """{"email":"tyche@tyche.com","externalAccountId":"01K23DN4Q5WD5BJ5FKGTG414EG"}"""

        (context, request, functions)

    let private ``given a request to link a non existing account`` () =
        let client = Mock<IAmazonDynamoDB>()
        setupMockLinkAccountWhenNotExistWrite client
        setupMockGetNonExistingAccountQuery client

        let functions =
            AccountFunctionWrapper(fun services -> services.AddSingleton<IAmazonDynamoDB>(client.Object) |> ignore)

        let context = TestLambdaContext()

        let request = APIGatewayHttpApiV2ProxyRequest()

        request.Body <- """{"email":"tyche@tyche.com","externalAccountId":"01K23DN4Q5WD5BJ5FKGTG414EG"}"""

        (context, request, functions)

    let private ``when requested`` (functions: AccountFunctionWrapper) request context =
        async { return! functions.LinkAccount(request, context) |> Async.AwaitTask }

    let private ``then the account is linked`` (response: APIGatewayHttpApiV2ProxyResponse) =
        response.StatusCode |> shouldEqual (int HttpStatusCode.OK)

    let private ``given a bad request to link an existing account`` () =
        let client = Mock<IAmazonDynamoDB>()
        setupMockLinkAccountWhenExistWrite client
        setupMockGetExistingAccountQuery client

        let functions =
            AccountFunctionWrapper(fun services -> services.AddSingleton<IAmazonDynamoDB>(client.Object) |> ignore)

        let context = TestLambdaContext()

        let request = APIGatewayHttpApiV2ProxyRequest()

        (context, request, functions)

    let private ``given a bad request to link a non existing account`` () =
        let client = Mock<IAmazonDynamoDB>()
        setupMockLinkAccountWhenNotExistWrite client
        setupMockGetNonExistingAccountQuery client

        let functions =
            AccountFunctionWrapper(fun services -> services.AddSingleton<IAmazonDynamoDB>(client.Object) |> ignore)

        let context = TestLambdaContext()

        let request = APIGatewayHttpApiV2ProxyRequest()

        (context, request, functions)

    let private ``then a bad response is returned`` (response: APIGatewayHttpApiV2ProxyResponse) =
        response.StatusCode |> shouldEqual (int HttpStatusCode.BadRequest)

    [<Fact>]
    let ``given a request to link an existing account when requested then the account is linked`` () =
        async {
            let context, request, functions = ``given a request to link an existing account`` ()
            let! response = ``when requested`` functions request context
            ``then the account is linked`` response
        }

    [<Fact>]
    let ``given a bad request to link an existing account when requested then a bad response is returned`` () =
        async {
            let context, request, functions =
                ``given a bad request to link an existing account`` ()

            let! response = ``when requested`` functions request context
            ``then a bad response is returned`` response
        }

    [<Fact>]
    let ``given a request to link a non existing account when requested then the account is linked`` () =
        async {
            let context, request, functions =
                ``given a request to link a non existing account`` ()

            let! response = ``when requested`` functions request context
            ``then the account is linked`` response
        }

    [<Fact>]
    let ``given a bad request to link a non existing account when requested then a bad response is returned`` () =
        async {
            let context, request, functions =
                ``given a bad request to link a non existing account`` ()

            let! response = ``when requested`` functions request context
            ``then a bad response is returned`` response
        }
