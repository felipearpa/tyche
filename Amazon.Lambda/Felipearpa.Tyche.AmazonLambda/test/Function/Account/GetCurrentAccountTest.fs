namespace Felipearpa.Tyche.AmazonLambda.Function.Account.Tests

#nowarn "3536"

open System.Collections.Generic
open System.Net
open Amazon.DynamoDBv2
open Amazon.DynamoDBv2.Model
open Amazon.Lambda.APIGatewayEvents
open Amazon.Lambda.TestUtilities
open Felipearpa.Tyche.AmazonLambda.Function
open Felipearpa.Tyche.AmazonLambda.Function.Tests
open FsUnitTyped
open Microsoft.Extensions.DependencyInjection
open Microsoft.Extensions.Logging
open Microsoft.Extensions.Logging.Abstractions
open Moq
open Xunit

module GetCurrentAccountTest =

    [<Literal>]
    let private callerAccountId = "01K1PX1TX2NM1HG851S1V0QG6N"

    [<Literal>]
    let private otherAccountId = "01K1PX1TX2NM1HG851S1V0QG6Z"

    [<Literal>]
    let private getByEmailIndex = "GetByEmail-index"

    let private setupGetCallerByEmail (client: Mock<IAmazonDynamoDB>) =
        let items =
            [ dict
                  [ "pk", AttributeValue(S = $"ACCOUNT#{callerAccountId}")
                    "accountId", AttributeValue(S = callerAccountId)
                    "email", AttributeValue(S = AuthorizationTestHelpers.testEmail)
                    "username", AttributeValue(S = AuthorizationTestHelpers.testEmail)
                    "externalAccountId", AttributeValue(S = "external-id") ] ]

        client
            .Setup(_.QueryAsync(It.Is<QueryRequest>(fun (request: QueryRequest) -> request.TableName = "Account")))
            .ReturnsAsync(QueryResponse(Items = (items |> List.map (fun it -> Dictionary it) |> ResizeArray)))
        |> ignore

    let private setupNoAccount (client: Mock<IAmazonDynamoDB>) =
        client
            .Setup(_.QueryAsync(It.Is<QueryRequest>(fun (request: QueryRequest) -> request.TableName = "Account")))
            .ReturnsAsync(QueryResponse(Items = ResizeArray<Dictionary<string, AttributeValue>>()))
        |> ignore

    let private buildFunctions (client: Mock<IAmazonDynamoDB>) =
        AccountFunction(fun services ->
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

    let private buildAuthorizedRequest () =
        let request = APIGatewayHttpApiV2ProxyRequest()
        AuthorizationTestHelpers.attachJwtClaim request AuthorizationTestHelpers.testEmail

    [<Fact>]
    let ``given an authenticated caller when the current account is requested then their canonical account is returned``
        ()
        =
        async {
            let client = Mock<IAmazonDynamoDB>()
            setupGetCallerByEmail client

            let functions = buildFunctions client

            let request = buildAuthorizedRequest ()

            let! response = functions.GetCurrentAccountAsync(request, TestLambdaContext()) |> Async.AwaitTask

            response.StatusCode |> shouldEqual (int HttpStatusCode.OK)
            response.Body |> shouldContainText callerAccountId
            response.Body |> shouldContainText AuthorizationTestHelpers.testEmail
            response.Body |> shouldContainText "external-id"
        }

    [<Fact>]
    let ``given a request without an authenticated principal when the current account is requested then Unauthorized is returned``
        ()
        =
        async {
            let client = Mock<IAmazonDynamoDB>()
            setupGetCallerByEmail client

            let functions = buildFunctions client

            let request = APIGatewayHttpApiV2ProxyRequest()

            let! response = functions.GetCurrentAccountAsync(request, TestLambdaContext()) |> Async.AwaitTask

            response.StatusCode |> shouldEqual (int HttpStatusCode.Unauthorized)
            client.Verify((fun client -> client.QueryAsync(It.IsAny<QueryRequest>())), Times.Never())
        }

    [<Fact>]
    let ``given an authenticated principal without an account when the current account is requested then Unauthorized is returned``
        ()
        =
        async {
            let client = Mock<IAmazonDynamoDB>()
            setupNoAccount client

            let functions = buildFunctions client

            let request = buildAuthorizedRequest ()

            let! response = functions.GetCurrentAccountAsync(request, TestLambdaContext()) |> Async.AwaitTask

            response.StatusCode |> shouldEqual (int HttpStatusCode.Unauthorized)
        }

    [<Fact>]
    let ``given request input naming another account when the current account is requested then only the caller's account is returned``
        ()
        =
        async {
            let client = Mock<IAmazonDynamoDB>()
            setupGetCallerByEmail client

            let functions = buildFunctions client

            let request = buildAuthorizedRequest ()
            request.PathParameters <- Dictionary(dict [ "accountId", otherAccountId ])
            request.QueryStringParameters <- Dictionary(dict [ "accountId", otherAccountId ])
            request.Body <- $"""{{"accountId":"{otherAccountId}"}}"""

            let! response = functions.GetCurrentAccountAsync(request, TestLambdaContext()) |> Async.AwaitTask

            response.StatusCode |> shouldEqual (int HttpStatusCode.OK)
            response.Body |> shouldContainText callerAccountId
            Assert.DoesNotContain(otherAccountId, response.Body)

            client.Verify(
                (fun client ->
                    client.QueryAsync(
                        It.Is<QueryRequest>(fun (r: QueryRequest) ->
                            r.TableName = "Account" && r.IndexName = getByEmailIndex)
                    )),
                Times.Once()
            )
        }
