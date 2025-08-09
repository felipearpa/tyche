namespace Felipearpa.Tyche.AmazonLambda.Tests.Pool

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
open Felipearpa.Core.Paging
open Felipearpa.Tyche.AmazonLambda
open Felipearpa.Tyche.Function.Response
open FsUnit.Xunit
open FsUnitTyped
open Microsoft.Extensions.DependencyInjection
open Moq
open Xunit

module GetFinishedBetsTest =

    let private ``given an existing pending bets`` () =
        let expectedBets: CursorPage<PoolGamblerBetResponse> =
            { Items =
                seq {
                    { PoolId = "01K1PX1TX2NM1HG851S1V0QG6N"
                      GamblerId = "01K1PX1TX2NM1HG851S1V0QG6N"
                      MatchId = "01K1PX1TX2NM1HG851S1V0QG6N"
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
                      isLocked = true }

                    { PoolId = "01K1PX1TX2NM1HG851S1V0QG6N"
                      GamblerId = "01K1PX1TX2NM1HG851S1V0QG6N"
                      MatchId = "01K1PX1TX2NM1HG851S1V0QG6N"
                      HomeTeamId = "01K1PX1TX2NM1HG851S1V0QG6N"
                      HomeTeamName = "Team Gamma"
                      HomeTeamScore = Some 0
                      HomeTeamBet = Some 0
                      AwayTeamId = "01K1PX1TX2NM1HG851S1V0QG6N"
                      AwayTeamName = "Team Delta"
                      AwayTeamScore = Some 2
                      AwayTeamBet = Some 2
                      Score = Some 1
                      MatchDateTime = DateTime(2024, 10, 13, 20, 30, 0, DateTimeKind.Utc)
                      isLocked = true }
                }
              Next = None }

        let client = Mock<IAmazonDynamoDB>()

        let items =
            [ dict
                  [ "pk", AttributeValue(S = "POOL#01K1PX1TX2NM1HG851S1V0QG6N")
                    "sk", AttributeValue(S = "GAMBLER#01K1PX1TX2NM1HG851S1V0QG6N")
                    "poolId", AttributeValue(S = "01K1PX1TX2NM1HG851S1V0QG6N")
                    "gamblerId", AttributeValue(S = "01K1PX1TX2NM1HG851S1V0QG6N")
                    "matchId", AttributeValue(S = "01K1PX1TX2NM1HG851S1V0QG6N")
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

              dict
                  [ "pk", AttributeValue(S = "POOL#01K1PX1TX2NM1HG851S1V0QG6N")
                    "sk", AttributeValue(S = "GAMBLER#01K1PX1TX2NM1HG851S1V0QG6N")
                    "poolId", AttributeValue(S = "01K1PX1TX2NM1HG851S1V0QG6N")
                    "gamblerId", AttributeValue(S = "01K1PX1TX2NM1HG851S1V0QG6N")
                    "matchId", AttributeValue(S = "01K1PX1TX2NM1HG851S1V0QG6N")
                    "homeTeamId", AttributeValue(S = "01K1PX1TX2NM1HG851S1V0QG6N")
                    "homeTeamName", AttributeValue(S = "Team Gamma")
                    "homeTeamScore", AttributeValue(N = "0")
                    "homeTeamBet", AttributeValue(N = "0")
                    "awayTeamId", AttributeValue(S = "01K1PX1TX2NM1HG851S1V0QG6N")
                    "awayTeamName", AttributeValue(S = "Team Delta")
                    "awayTeamScore", AttributeValue(N = "2")
                    "awayTeamBet", AttributeValue(N = "2")
                    "score", AttributeValue(N = "1")
                    "matchDateTime", AttributeValue(S = "2024-10-13T20:30:00Z") ] ]

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

        (expectedBets, context, request, functions)

    let private ``when requesting its information`` (functions: PoolFunctionWrapper) request context =
        async { return! functions.GetFinishedBetsAsync(request, context) |> Async.AwaitTask }

    let private ``then the pending bets are returned``
        (response: APIGatewayHttpApiV2ProxyResponse)
        (expectedBets: CursorPage<PoolGamblerBetResponse>)
        =
        response.StatusCode |> should equal (int HttpStatusCode.OK)

        let serializer = JsonSerializer() :> ISerializer

        let actualBets =
            serializer.Deserialize<CursorPage<PoolGamblerBetResponse>>(response.Body)

        actualBets.Items |> Seq.toList |> shouldEqual (expectedBets.Items |> Seq.toList)

    let private ``given a request without poolId`` () =
        let functions = PoolFunctionWrapper()

        let context = TestLambdaContext()

        let request = APIGatewayHttpApiV2ProxyRequest()

        (context, request, functions)

    let private ``then a bad response is returned`` (response: APIGatewayHttpApiV2ProxyResponse) =
        response.StatusCode |> shouldEqual (int HttpStatusCode.BadRequest)

    [<Fact>]
    let ``given an existing finished bets when requesting its information then the finished bets are returned`` () =
        async {
            let expectedBets, context, request, functions =
                ``given an existing pending bets`` ()

            let! response = ``when requesting its information`` functions request context

            ``then the pending bets are returned`` response expectedBets
        }

    [<Fact>]
    let ``given a request without path parameters when requesting its information then bad response is returned`` () =
        async {
            let context, request, functions = ``given a request without poolId`` ()
            let! response = ``when requesting its information`` functions request context
            ``then a bad response is returned`` response
        }
