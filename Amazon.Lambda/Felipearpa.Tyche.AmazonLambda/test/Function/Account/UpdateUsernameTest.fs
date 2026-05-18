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

module UpdateUsernameTest =

    [<Literal>]
    let private callerAccountId = "01K1PX1TX2NM1HG851S1V0QG6N"

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

    let private setupNoPoolMembership (client: Mock<IAmazonDynamoDB>) =
        client
            .Setup(_.QueryAsync(It.Is<QueryRequest>(fun (request: QueryRequest) -> request.TableName = "Pool")))
            .ReturnsAsync(QueryResponse(Items = ResizeArray<Dictionary<string, AttributeValue>>()))
        |> ignore

    let private setupAccountUpdate (client: Mock<IAmazonDynamoDB>) =
        client.Setup(_.UpdateItemAsync(It.IsAny<UpdateItemRequest>())).ReturnsAsync(UpdateItemResponse())
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

    let private buildAuthorizedRequest (body: string) =
        let request = APIGatewayHttpApiV2ProxyRequest()
        request.Body <- body
        AuthorizationTestHelpers.attachJwtClaim request AuthorizationTestHelpers.testEmail

    [<Fact>]
    let ``given the caller updating their own username when requested then NoContent is returned`` () =
        async {
            let client = Mock<IAmazonDynamoDB>()
            setupGetCallerByEmail client
            setupNoPoolMembership client
            setupAccountUpdate client

            let functions = buildFunctions client

            let request =
                buildAuthorizedRequest $"""{{"accountId":"{callerAccountId}","username":"newname"}}"""

            let! response = functions.UpdateUsernameAsync(request, TestLambdaContext()) |> Async.AwaitTask

            response.StatusCode |> shouldEqual (int HttpStatusCode.NoContent)

            client.Verify(
                (fun client ->
                    client.UpdateItemAsync(
                        It.Is<UpdateItemRequest>(fun (r: UpdateItemRequest) -> r.TableName = "Account")
                    )),
                Times.Once()
            )
        }

    [<Fact>]
    let ``given a caller updating someone else's username when requested then Forbidden is returned`` () =
        async {
            let client = Mock<IAmazonDynamoDB>()
            setupGetCallerByEmail client
            setupAccountUpdate client

            let functions = buildFunctions client

            let request =
                buildAuthorizedRequest """{"accountId":"01K1PX1TX2NM1HG851S1V0QG6Z","username":"newname"}"""

            let! response = functions.UpdateUsernameAsync(request, TestLambdaContext()) |> Async.AwaitTask

            response.StatusCode |> shouldEqual (int HttpStatusCode.Forbidden)

            client.Verify((fun client -> client.UpdateItemAsync(It.IsAny<UpdateItemRequest>())), Times.Never())
        }
