namespace Felipearpa.Tyche.AmazonLambda.Function.Pool.Tests

#nowarn "3536"

open System
open System.Collections.Generic
open System.Net
open System.Threading
open System.Threading.Tasks
open Amazon.DynamoDBv2
open Amazon.DynamoDBv2.Model
open Amazon.Lambda.APIGatewayEvents
open Amazon.Lambda.TestUtilities
open Felipearpa.Tyche.AmazonLambda.Function
open Felipearpa.Tyche.AmazonLambda.Function.Tests.AuthorizationTestHelpers
open FsUnitTyped
open Microsoft.Extensions.DependencyInjection
open Microsoft.Extensions.Logging
open Microsoft.Extensions.Logging.Abstractions
open Moq
open Xunit

/// Routing of GET /pools/{poolId}/matches/{matchId}/bets by its sort mode.
module GetMatchBetsTest =

    let private poolId = "01K1PX1TX2NM1HG851S1V0P001"
    let private matchId = "01K1PX1TX2NM1HG851S1V0M001"
    let private layoutId = "01K1PX1TX2NM1HG851S1V0Y001"
    let private callerId = "01K1PX1TX2NM1HG851S1V0GA01"

    let private s (value: string) = AttributeValue(S = value)

    let private item (entries: (string * AttributeValue) list) = Dictionary(dict entries)

    let private respondToGets (client: Mock<IAmazonDynamoDB>) (readiness: string) =
        client
            .Setup(fun c -> c.GetItemAsync(It.IsAny<GetItemRequest>(), It.IsAny<CancellationToken>()))
            .Returns(fun (request: GetItemRequest) (_: CancellationToken) ->
                let sk = request.Key["sk"].S

                let found =
                    if sk = $"POOL#{poolId}" then
                        Some(
                            item
                                [ "pk", s $"POOL#{poolId}"
                                  "sk", s $"POOL#{poolId}"
                                  "poolId", s poolId
                                  "poolLayoutId", s layoutId
                                  "rankingReadiness", AttributeValue(M = Dictionary(dict [ "state", s readiness ])) ]
                        )
                    elif sk = $"GAMBLER#{callerId}" then
                        Some(
                            item
                                [ "pk", s $"POOL#{poolId}"
                                  "sk", s sk
                                  "gamblerId", s callerId
                                  "gamblerUsername", s "a"
                                  "gamblerEmail", s "a@example.com"
                                  "poolLayoutId", s layoutId ]
                        )
                    elif sk = $"MATCH#{matchId}" then
                        Some(
                            item
                                [ "pk", s $"POOLLAYOUT#{layoutId}"
                                  "sk", s sk
                                  "matchId", s matchId
                                  "poolLayoutId", s layoutId
                                  "homeTeamId", s "COL"
                                  "homeTeamName", s "Colombia"
                                  "awayTeamId", s "ARG"
                                  "awayTeamName", s "Argentina"
                                  "matchDateTime", s "2024-10-12T18:00:00Z"
                                  "poolLayoutVersion", AttributeValue(N = "1") ]
                        )
                    else
                        None

                Task.FromResult(
                    match found with
                    | Some value -> GetItemResponse(Item = value, IsItemSet = true)
                    | None -> GetItemResponse(IsItemSet = false)
                ))
        |> ignore

    let private functions (client: Mock<IAmazonDynamoDB>) =
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

            services.AddSingleton<IAmazonDynamoDB>(client.Object) |> ignore
            registerAuthAsCaller services callerId)

    let private request (sort: string option) (authenticated: bool) =
        let request = APIGatewayHttpApiV2ProxyRequest()
        request.PathParameters <- Dictionary(dict [ "poolId", poolId; "matchId", matchId ])

        request.QueryStringParameters <-
            Dictionary(sort |> Option.map (fun value -> dict [ "sort", value ]) |> Option.defaultValue (dict []))

        if authenticated then attachJwtClaim request testEmail |> ignore
        request

    let private run (functions: PoolFunction) request =
        functions.GetMatchBetsAsync(request, TestLambdaContext()) |> Async.AwaitTask |> Async.RunSynchronously

    [<Fact>]
    let ``without a sort the legacy match index order is kept`` () =
        let client = Mock<IAmazonDynamoDB>()
        let queried = List<QueryRequest>()

        client
            .Setup(fun c -> c.QueryAsync(It.IsAny<QueryRequest>(), It.IsAny<CancellationToken>()))
            .Callback<QueryRequest, CancellationToken>(fun request _ -> queried.Add request)
            .ReturnsAsync(QueryResponse(Items = List()))
        |> ignore

        let response = run (functions client) (request None false)

        response.StatusCode |> shouldEqual (int HttpStatusCode.OK)
        queried |> Seq.map _.IndexName |> List.ofSeq |> shouldEqual [ "GetPoolGamblerScoresByMatch-index" ]

    [<Fact>]
    let ``an unknown sort is a bad request`` () =
        let response = run (functions (Mock<IAmazonDynamoDB>())) (request (Some "match-points") true)
        response.StatusCode |> shouldEqual (int HttpStatusCode.BadRequest)

    [<Fact>]
    let ``the ranked mode requires an authenticated caller`` () =
        let response = run (functions (Mock<IAmazonDynamoDB>())) (request (Some "pool-score-desc") false)
        response.StatusCode |> shouldEqual (int HttpStatusCode.Unauthorized)

    [<Fact>]
    let ``a pool that is not ready fails recoverably without querying any index`` () =
        let client = Mock<IAmazonDynamoDB>()
        respondToGets client "NOT_READY"

        let response = run (functions client) (request (Some "pool-score-desc") true)

        response.StatusCode |> shouldEqual 503
        response.Body.Contains "RANKING_NOT_READY" |> shouldEqual true
        client.Verify((fun c -> c.QueryAsync(It.IsAny<QueryRequest>(), It.IsAny<CancellationToken>())), Times.Never())

    [<Fact>]
    let ``an unreadable ranking index is a recoverable outage, not another order`` () =
        let client = Mock<IAmazonDynamoDB>()
        respondToGets client "READY"

        client
            .Setup(fun c -> c.QueryAsync(It.IsAny<QueryRequest>(), It.IsAny<CancellationToken>()))
            .Returns(Task.FromException<QueryResponse>(AmazonDynamoDBException "The table does not have the specified index"))
        |> ignore

        let response = run (functions client) (request (Some "pool-score-desc") true)

        response.StatusCode |> shouldEqual 503
        response.Body.Contains "RANKING_UNAVAILABLE" |> shouldEqual true

    [<Fact>]
    let ``a ranked page is read from the ranking index`` () =
        let client = Mock<IAmazonDynamoDB>()
        respondToGets client "READY"
        let queried = List<QueryRequest>()

        client
            .Setup(fun c -> c.QueryAsync(It.IsAny<QueryRequest>(), It.IsAny<CancellationToken>()))
            .Callback<QueryRequest, CancellationToken>(fun request _ -> queried.Add request)
            .ReturnsAsync(QueryResponse(Items = List()))
        |> ignore

        let response = run (functions client) (request (Some "pool-score-desc") true)

        response.StatusCode |> shouldEqual (int HttpStatusCode.OK)
        queried |> Seq.map _.IndexName |> List.ofSeq |> shouldEqual [ "GetPoolMatchBetsByTotal-index" ]
