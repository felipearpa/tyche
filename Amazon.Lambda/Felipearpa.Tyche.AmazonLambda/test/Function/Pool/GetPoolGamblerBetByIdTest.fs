namespace Felipearpa.Tyche.AmazonLambda.Function.Pool.Tests

#nowarn "3536"

open System
open System.Collections.Generic
open System.Net
open Amazon.DynamoDBv2
open Amazon.DynamoDBv2.Model
open Amazon.Lambda.APIGatewayEvents
open Amazon.Lambda.TestUtilities
open Felipearpa.Core
open Felipearpa.Core.Json
open Felipearpa.Tyche.AmazonLambda.Function
open Felipearpa.Tyche.Function.Response
open FsUnit.Xunit
open FsUnitTyped
open Microsoft.Extensions.DependencyInjection
open Microsoft.Extensions.Logging
open Microsoft.Extensions.Logging.Abstractions
open Moq
open Xunit

module GetPoolGamblerBetByIdTest =

    let private ``given an existing PoolGamblerBet`` () =
        let expectedBet: PoolGamblerBetResponse =
            { PoolId = "01K1PX1TX2NM1HG851S1V0QG6N"
              GamblerId = "01K1PX1TX2NM1HG851S1V0QG6N"
              GamblerUsername = "felipearcila@gmail.com"
              MatchId = "01K1PX1TX2NM1HG851S1V0QG6N"
              PoolLayoutId = "01K1PX1TX2NM1HG851S1V0QG6N"
              HomeTeamId = "01K1PX1TX2NM1HG851S1V0QG6N"
              HomeTeamName = "Team Alpha"
              HomeTeamScore = Some 2
              HomeTeamBet = Some 1
              AwayTeamId = "01K1PX1TX2NM1HG851S1V0QG6N"
              AwayTeamName = "Team Beta"
              AwayTeamScore = Some 1
              AwayTeamBet = Some 1
              Score = Some 3
              MatchDateTime = DateTime(2024, 10, 12, 18, 0, 0, DateTimeKind.Utc)
              Round = "Fase de grupos"
              isLocked = true
              isComputed = false }

        let client = Mock<IAmazonDynamoDB>()

        let item =
            dict
                [ "pk", AttributeValue(S = "GAMBLER#01K1PX1TX2NM1HG851S1V0QG6N#POOL#01K1PX1TX2NM1HG851S1V0QG6N")
                  "sk", AttributeValue(S = "MATCH#01K1PX1TX2NM1HG851S1V0QG6N")
                  "poolId", AttributeValue(S = "01K1PX1TX2NM1HG851S1V0QG6N")
                  "gamblerId", AttributeValue(S = "01K1PX1TX2NM1HG851S1V0QG6N")
                  "gamblerUsername", AttributeValue(S = "felipearcila@gmail.com")
                  "matchId", AttributeValue(S = "01K1PX1TX2NM1HG851S1V0QG6N")
                  "poolLayoutId", AttributeValue(S = "01K1PX1TX2NM1HG851S1V0QG6N")
                  "homeTeamId", AttributeValue(S = "01K1PX1TX2NM1HG851S1V0QG6N")
                  "homeTeamName", AttributeValue(S = "Team Alpha")
                  "homeTeamScore", AttributeValue(N = "2")
                  "homeTeamBet", AttributeValue(N = "1")
                  "awayTeamId", AttributeValue(S = "01K1PX1TX2NM1HG851S1V0QG6N")
                  "awayTeamName", AttributeValue(S = "Team Beta")
                  "awayTeamScore", AttributeValue(N = "1")
                  "awayTeamBet", AttributeValue(N = "1")
                  "score", AttributeValue(N = "3")
                  "matchDateTime", AttributeValue(S = "2024-10-12T18:00:00Z") ]

        client
            .Setup(_.GetItemAsync(It.IsAny<GetItemRequest>(), It.IsAny<Threading.CancellationToken>()))
            .ReturnsAsync(GetItemResponse(Item = Dictionary item))
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

        request.PathParameters <-
            dict
                [ "poolId", "01K1PX1TX2NM1HG851S1V0QG6N"
                  "gamblerId", "01K1PX1TX2NM1HG851S1V0QG6N"
                  "matchId", "01K1PX1TX2NM1HG851S1V0QG6N" ]

        (expectedBet, context, request, functions)

    let private ``when requesting its information`` (functions: PoolFunction) request context =
        async { return! functions.GetPoolGamblerBetByIdAsync(request, context) |> Async.AwaitTask }

    let private ``then the associated PoolGamblerBet is returned``
        (response: APIGatewayHttpApiV2ProxyResponse)
        (expectedBet: PoolGamblerBetResponse)
        =
        response.StatusCode |> should equal (int HttpStatusCode.OK)

        let serializer = JsonSerializer() :> ISerializer

        let actualBet = serializer.Deserialize<PoolGamblerBetResponse>(response.Body)

        actualBet |> shouldEqual expectedBet

    let private ``given a non existing PoolGamblerBet`` () =
        let client = Mock<IAmazonDynamoDB>()

        client
            .Setup(_.GetItemAsync(It.IsAny<GetItemRequest>(), It.IsAny<Threading.CancellationToken>()))
            .ReturnsAsync(GetItemResponse(IsItemSet = false))
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

        request.PathParameters <-
            dict
                [ "poolId", "01K1PX1TX2NM1HG851S1V0QG6N"
                  "gamblerId", "01K1PX1TX2NM1HG851S1V0QG6N"
                  "matchId", "01K1PX1TX2NM1HG851S1V0QG6N" ]

        (context, request, functions)

    let private ``then a not found response is returned`` (response: APIGatewayHttpApiV2ProxyResponse) =
        response.StatusCode |> shouldEqual (int HttpStatusCode.NotFound)

    let private ``given a request without path parameters`` () =
        let functions = PoolFunction()

        let context = TestLambdaContext()

        let request = APIGatewayHttpApiV2ProxyRequest()

        (context, request, functions)

    let private ``then a bad response is returned`` (response: APIGatewayHttpApiV2ProxyResponse) =
        response.StatusCode |> shouldEqual (int HttpStatusCode.BadRequest)

    [<Fact>]
    let ``given an existing PoolGamblerBet when requesting its information then the associated PoolGamblerBet is returned``
        ()
        =
        async {
            let expectedBet, context, request, functions = ``given an existing PoolGamblerBet`` ()

            let! response = ``when requesting its information`` functions request context

            ``then the associated PoolGamblerBet is returned`` response expectedBet
        }

    [<Fact>]
    let ``given a non existing PoolGamblerBet when requesting its information then a not found response is returned``
        ()
        =
        async {
            let context, request, functions = ``given a non existing PoolGamblerBet`` ()

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
