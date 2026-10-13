namespace Felipearpa.Tyche.AmazonLambda.Function.Tests

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
open Felipearpa.Core
open Felipearpa.Core.Json
open Felipearpa.Core.Paging
open Felipearpa.Tyche.AmazonLambda.Function
open Felipearpa.Tyche.Function.Response
open FsUnitTyped
open Microsoft.Extensions.DependencyInjection
open Microsoft.Extensions.Logging
open Microsoft.Extensions.Logging.Abstractions
open Moq
open Xunit

/// The leaderboard (GET /pools/{poolId}/gamblers), My pools (GET /gamblers/{gamblerId}/pools),
/// and a gambler's score (GET /pools/{poolId}/gamblers/{gamblerId}) carry the pool's trend
/// kickoff group, read from the pool root and its layout.
module TrendGroupScoreTest =

    let private settledPool = "01K1PX1TX2NM1HG851S1V0P001"
    let private unsettledPool = "01K1PX1TX2NM1HG851S1V0P002"
    let private settledLayout = "01K1PX1TX2NM1HG851S1V0Y001"
    let private unsettledLayout = "01K1PX1TX2NM1HG851S1V0Y002"
    let private gambler = "01K1PX1TX2NM1HG851S1V0GA01"
    let private rival = "01K1PX1TX2NM1HG851S1V0GB01"
    let private trendKickoff = DateTime(2026, 6, 12, 17, 0, 0, DateTimeKind.Utc)

    let private s (value: string) = AttributeValue(S = value)
    let private n (value: int) = AttributeValue(N = string value)

    let private membership pool layout gamblerId position beforePosition score =
        Dictionary(
            dict
                [ "pk", s $"POOL#{pool}"
                  "sk", s $"GAMBLER#{gamblerId}"
                  "poolId", s pool
                  "poolName", s $"Pool {pool.Substring 22}"
                  "poolLayoutId", s layout
                  "gamblerId", s gamblerId
                  "gamblerUsername", s $"user-{gamblerId.Substring 22}"
                  "status", s "OPENED"
                  "position", n position
                  "beforePosition", n beforePosition
                  "score", n score ]
        )

    let private roots =
        [ settledPool,
          Dictionary(
              dict
                  [ "pk", s $"POOL#{settledPool}"
                    "sk", s $"POOL#{settledPool}"
                    "poolId", s settledPool
                    "poolLayoutId", s settledLayout
                    "gamblerCount", n 2
                    "trendKickoffDateTime", s "2026-06-12T17:00:00.0000000Z" ]
          )
          unsettledPool,
          Dictionary(
              dict
                  [ "pk", s $"POOL#{unsettledPool}"
                    "sk", s $"POOL#{unsettledPool}"
                    "poolId", s unsettledPool
                    "poolLayoutId", s unsettledLayout
                    "gamblerCount", n 1 ]
          ) ]
        |> Map.ofList

    let private layoutMatch (matchId: string) (kickoff: string) (result: (int * int) option) =
        let item =
            Dictionary(
                dict
                    [ "pk", s $"POOLLAYOUT#{settledLayout}"
                      "sk", s $"MATCH#{matchId}"
                      "matchId", s matchId
                      "poolLayoutId", s settledLayout
                      "homeTeamId", s "COL"
                      "homeTeamName", s "Colombia"
                      "awayTeamId", s "ARG"
                      "awayTeamName", s "Argentina"
                      "matchDateTime", s kickoff
                      "poolLayoutVersion", n 1
                      "round", s "Fase de grupos" ]
            )

        result
        |> Option.iter (fun (home, away) ->
            item["homeTeamScore"] <- n home
            item["awayTeamScore"] <- n away
            item["resultVersion"] <- n 1)

        item

    /// The trend group has two matches, one of them stored with another offset; one has a
    /// result. Another kickoff later that day is a separate group.
    let private settledLayoutMatches =
        [ layoutMatch "01K1PX1TX2NM1HG851S1V0M001" "2026-06-12T17:00:00Z" (Some(1, 0))
          layoutMatch "01K1PX1TX2NM1HG851S1V0M002" "2026-06-12T19:00:00+02:00" None
          layoutMatch "01K1PX1TX2NM1HG851S1V0M003" "2026-06-12T20:00:00Z" (Some(2, 2)) ]

    let private leaderboardRows =
        [ membership settledPool settledLayout rival 1 2 30
          membership settledPool settledLayout gambler 2 1 24 ]

    let private gamblerRows =
        [ membership settledPool settledLayout gambler 2 1 24
          membership unsettledPool unsettledLayout gambler 1 1 0 ]

    let private query (request: QueryRequest) =
        let items =
            match request.IndexName, request.TableName with
            | "GetPoolGamblerScoresByPool-index", _ -> leaderboardRows
            | "GetPoolGamblerScoresByGambler-index", _ -> gamblerRows
            | null, "Pool" ->
                gamblerRows
                |> List.filter (fun row ->
                    row["pk"].S = request.ExpressionAttributeValues[":pk"].S
                    && row["sk"].S = request.ExpressionAttributeValues[":sk"].S)
            | null, "PoolLayout" when request.ExpressionAttributeValues[":pk"].S = $"POOLLAYOUT#{settledLayout}" ->
                settledLayoutMatches
            | null, "PoolLayout" -> []
            | other -> failwith $"unexpected query {other}"

        Task.FromResult(QueryResponse(Items = ResizeArray items))

    let private batchGet (request: BatchGetItemRequest) =
        let found =
            request.RequestItems["Pool"].Keys
            |> Seq.choose (fun key -> roots |> Map.tryFind (key["pk"].S.Substring "POOL#".Length))
            |> ResizeArray

        Task.FromResult(BatchGetItemResponse(Responses = Dictionary(dict [ "Pool", found ])))

    let private client () =
        let mock = Mock<IAmazonDynamoDB>()

        mock
            .Setup(fun c -> c.QueryAsync(It.IsAny<QueryRequest>(), It.IsAny<CancellationToken>()))
            .Returns(fun (request: QueryRequest) (_: CancellationToken) -> query request)
        |> ignore

        mock
            .Setup(fun c -> c.BatchGetItemAsync(It.IsAny<BatchGetItemRequest>(), It.IsAny<CancellationToken>()))
            .Returns(fun (request: BatchGetItemRequest) (_: CancellationToken) -> batchGet request)
        |> ignore

        mock

    let private configure (dynamo: IAmazonDynamoDB) (services: IServiceCollection) =
        services.AddLogging(fun builder ->
            builder.ClearProviders() |> ignore

            builder.AddProvider(
                { new ILoggerProvider with
                    member _.CreateLogger _ = NullLogger.Instance
                    member _.Dispose() = () }
            )
            |> ignore)
        |> ignore

        services.AddSingleton<IAmazonDynamoDB>(dynamo) |> ignore

    let private request (parameters: (string * string) list) =
        APIGatewayHttpApiV2ProxyRequest(PathParameters = dict parameters)

    let private read (response: APIGatewayHttpApiV2ProxyResponse) =
        response.StatusCode |> shouldEqual (int HttpStatusCode.OK)
        let page = (JsonSerializer() :> ISerializer).Deserialize<CursorPage<PoolGamblerScoreResponse>>(response.Body)
        page.Items |> List.ofSeq

    let private leaderboard (mock: Mock<IAmazonDynamoDB>) =
        async {
            let! response =
                PoolFunction(configure mock.Object).GetGamblersByPoolIdAsync(request [ "poolId", settledPool ], TestLambdaContext())
                |> Async.AwaitTask

            return response
        }

    let private myPools (mock: Mock<IAmazonDynamoDB>) =
        async {
            let! response =
                GamblerFunction(configure mock.Object).GetPoolsByGamblerIdAsync(request [ "gamblerId", gambler ], TestLambdaContext())
                |> Async.AwaitTask

            return response
        }

    let private scoreById (mock: Mock<IAmazonDynamoDB>) (pool: string) =
        async {
            let! response =
                PoolFunction(configure mock.Object)
                    .GetPoolGamblerScoreByIdAsync(request [ "poolId", pool; "gamblerId", gambler ], TestLambdaContext())
                |> Async.AwaitTask

            response.StatusCode |> shouldEqual (int HttpStatusCode.OK)
            return (JsonSerializer() :> ISerializer).Deserialize<PoolGamblerScoreResponse>(response.Body)
        }

    let private layoutQueries (mock: Mock<IAmazonDynamoDB>) =
        mock.Invocations
        |> Seq.filter (fun invocation -> invocation.Method.Name = "QueryAsync")
        |> Seq.map (fun invocation -> invocation.Arguments[0] :?> QueryRequest)
        |> Seq.filter (fun query -> query.TableName = "PoolLayout")
        |> Seq.length

    let private expectedGroup =
        { KickoffDateTime = trendKickoff
          ComputedMatches = 1
          TotalMatches = 2 }

    [<Fact>]
    let ``the leaderboard carries the trend group and keeps positions`` () =
        async {
            let mock = client ()
            let! response = leaderboard mock
            let rows = read response

            rows |> List.map _.TrendGroup |> shouldEqual [ Some expectedGroup; Some expectedGroup ]
            rows |> List.map (fun row -> row.Position, row.BeforePosition) |> shouldEqual [ Some 1, Some 2; Some 2, Some 1 ]
            // One layout read serves every row of the page.
            layoutQueries mock |> shouldEqual 1
            response.Body |> shouldContainText "\"kickoffDateTime\":\"2026-06-12T17:00:00Z\""
        }

    [<Fact>]
    let ``My pools carries the same trend group as the leaderboard and none for a never-settled pool`` () =
        async {
            let! leaderboardResponse = leaderboard (client ())
            let fromLeaderboard = read leaderboardResponse |> List.find (fun row -> row.GamblerId = gambler)

            let mock = client ()
            let! myPoolsResponse = myPools mock
            let rows = read myPoolsResponse
            let fromMyPools = rows |> List.find (fun row -> row.PoolId = settledPool)

            (fromMyPools.Position, fromMyPools.BeforePosition, fromMyPools.TrendGroup)
            |> shouldEqual (fromLeaderboard.Position, fromLeaderboard.BeforePosition, fromLeaderboard.TrendGroup)

            fromMyPools.TrendGroup |> shouldEqual (Some expectedGroup)

            let neverSettled = rows |> List.find (fun row -> row.PoolId = unsettledPool)
            neverSettled.TrendGroup |> shouldEqual None
            (neverSettled.Position, neverSettled.BeforePosition) |> shouldEqual (Some 1, Some 1)
            // Only the settled pool's layout is read.
            layoutQueries mock |> shouldEqual 1
        }

    [<Fact>]
    let ``a gambler's score carries the same trend group as the leaderboard and My pools`` () =
        async {
            let! leaderboardResponse = leaderboard (client ())
            let fromLeaderboard = read leaderboardResponse |> List.find (fun row -> row.GamblerId = gambler)
            let! myPoolsResponse = myPools (client ())
            let fromMyPools = read myPoolsResponse |> List.find (fun row -> row.PoolId = settledPool)

            let! fromScore = scoreById (client ()) settledPool

            let movement (row: PoolGamblerScoreResponse) = row.Position, row.BeforePosition, row.TrendGroup
            movement fromScore |> shouldEqual (movement fromLeaderboard)
            movement fromScore |> shouldEqual (movement fromMyPools)
            movement fromScore |> shouldEqual (Some 2, Some 1, Some expectedGroup)
        }

    [<Fact>]
    let ``a gambler's score in a never-settled pool has no trend group`` () =
        async {
            let mock = client ()
            let! fromScore = scoreById mock unsettledPool

            fromScore.TrendGroup |> shouldEqual None
            (fromScore.Position, fromScore.BeforePosition) |> shouldEqual (Some 1, Some 1)
            layoutQueries mock |> shouldEqual 0
        }
