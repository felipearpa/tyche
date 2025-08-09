namespace Felipearpa.Tyche.AmazonLambda.Tests.Bet

#nowarn "3536"

open System
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

    let private setupMockBetWrite (client: Mock<IAmazonDynamoDB>) =
        let item =
            dict
                [ "poolId", AttributeValue(S = "01K0DCFFB08W35AW5Q6F82R6NQ")
                  "gamblerId", AttributeValue(S = "01K0DCFFB08W35AW5Q6F82R6NQ")
                  "matchId", AttributeValue(S = "01K0DCFFB08W35AW5Q6F82R6NQ")
                  "homeTeamId", AttributeValue(S = "01K0DCFFB08W35AW5Q6F82R6NQ")
                  "homeTeamName", AttributeValue(S = "Home FC")
                  "awayTeamId", AttributeValue(S = "01K0DCFFB08W35AW5Q6F82R6NQ")
                  "awayTeamName", AttributeValue(S = "Away United")
                  "homeTeamScore", AttributeValue(N = "2")
                  "awayTeamScore", AttributeValue(N = "1")
                  "homeTeamBet", AttributeValue(N = "2")
                  "awayTeamBet", AttributeValue(N = "1")
                  "score", AttributeValue(N = "15")
                  "matchDateTime", AttributeValue(S = DateTime.UtcNow.AddDays(-1.0).ToString("yyyy-MM-ddTHH:mm:ssZ")) ]

        client
            .Setup(_.UpdateItemAsync(It.IsAny<UpdateItemRequest>()))
            .ReturnsAsync(UpdateItemResponse(Attributes = (Dictionary item)))
        |> ignore

    let private ``given a request to bet`` () =
        let client = Mock<IAmazonDynamoDB>()
        setupMockBetWrite client

        let functions =
            BetFunctionWrapper(fun services -> services.AddSingleton<IAmazonDynamoDB>(client.Object) |> ignore)

        let context = TestLambdaContext()

        let request = APIGatewayHttpApiV2ProxyRequest()

        request.Body <-
            """{"poolId":"01K23DN4Q5WD5BJ5FKGTG414EG","gamblerId":"01K23DN4Q5WD5BJ5FKGTG414EG","matchId":"01K23DN4Q5WD5BJ5FKGTG414EH","homeTeamBet":2,"awayTeamBet":1}"""

        (context, request, functions)

    let private ``when requested`` (functions: BetFunctionWrapper) request context =
        async { return! functions.Bet(request, context) |> Async.AwaitTask }

    let private ``then the bet is added`` (response: APIGatewayHttpApiV2ProxyResponse) =
        response.StatusCode |> shouldEqual (int HttpStatusCode.OK)

    let private ``given a bad request to bet`` () =
        let client = Mock<IAmazonDynamoDB>()
        setupMockBetWrite client

        let functions =
            BetFunctionWrapper(fun services -> services.AddSingleton<IAmazonDynamoDB>(client.Object) |> ignore)

        let context = TestLambdaContext()

        let request = APIGatewayHttpApiV2ProxyRequest()

        (context, request, functions)

    let private ``then a bad response is returned`` (response: APIGatewayHttpApiV2ProxyResponse) =
        response.StatusCode |> shouldEqual (int HttpStatusCode.BadRequest)

    [<Fact>]
    let ``given a request to bet when requested then the bet is added`` () =
        async {
            let context, request, functions = ``given a request to bet`` ()
            let! response = ``when requested`` functions request context
            ``then the bet is added`` response
        }

    [<Fact>]
    let ``given a bad request to bet when requested then a bad response is returned`` () =
        async {
            let context, request, functions = ``given a bad request to bet`` ()
            let! response = ``when requested`` functions request context
            ``then a bad response is returned`` response
        }
