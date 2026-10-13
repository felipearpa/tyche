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
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.AmazonLambda.Function
open Felipearpa.Tyche.AmazonLambda.Function.Tests.AuthorizationTestHelpers
open FsUnit.Xunit
open FsUnitTyped
open Microsoft.Extensions.DependencyInjection
open Microsoft.Extensions.Logging
open Microsoft.Extensions.Logging.Abstractions
open Moq
open Xunit

/// The Lambda host's handling of `next` tokens on every shared-codec endpoint: unusable tokens
/// get 400 `INVALID_CURSOR` before the paginated query runs; DynamoDB failures for a usable
/// token keep their existing handling.
module InvalidCursorTest =

    let private poolId = "01K1PX1TX2NM1HG851S1V0P001"
    let private otherPoolId = "01K1PX1TX2NM1HG851S1V0P002"
    let private gamblerId = "01K1PX1TX2NM1HG851S1V0GA01"
    let private otherGamblerId = "01K1PX1TX2NM1HG851S1V0GA02"
    let private matchId = "01K1PX1TX2NM1HG851S1V0M001"
    let private otherMatchId = "01K1PX1TX2NM1HG851S1V0M002"
    let private layoutId = "01K1PX1TX2NM1HG851S1V0Y001"

    let private token (attributes: (string * ScalarKeyValue) list) =
        match ContinuationKey.tryCreate (Map attributes) with
        | Ok key -> ContinuationToken.encode key
        | Error error -> failwith $"invalid test key: %A{error}"

    let private legacy pk sk = $"{{\"pk\":\"{pk}\",\"sk\":\"{sk}\"}}"

    let private betToken pool gambler dateAttribute =
        token
            [ "pk", S $"GAMBLER#{gambler}#POOL#{pool}"
              "sk", S $"MATCH#{matchId}"
              dateAttribute, S "2026-06-12T17:00:00.0000000Z" ]

    let private scoreToken pool gambler =
        token [ "pk", S $"POOL#{pool}"; "sk", S $"GAMBLER#{gambler}"; "score", N "5" ]

    let private memberToken pool =
        token [ "pk", S $"POOL#{pool}"; "sk", S $"GAMBLER#{gamblerId}"; "gamblerUsername", S "bea" ]

    let private matchBetToken matchValue =
        token
            [ "pk", S $"GAMBLER#{gamblerId}#POOL#{poolId}"
              "sk", S $"MATCH#{matchValue}"
              "getPoolGamblerScoresByMatchPk", S $"MATCH#{matchValue}"
              "getPoolGamblerScoresByMatchSk", S $"POOL#{poolId}#GAMBLER#{gamblerId}" ]

    let private layoutToken status =
        token
            [ "pk", S $"POOLLAYOUT#{layoutId}"
              "sk", S $"POOLLAYOUT#{layoutId}"
              "status", S status
              "startDateTime", S "2026-06-11T00:00:00Z" ]

    let private poolRoot =
        Dictionary(
            dict
                [ "pk", AttributeValue(S = $"POOL#{poolId}")
                  "sk", AttributeValue(S = $"POOL#{poolId}")
                  "poolId", AttributeValue(S = poolId)
                  "poolName", AttributeValue(S = "Pool")
                  "poolLayoutId", AttributeValue(S = layoutId)
                  "creatorGamblerId", AttributeValue(S = gamblerId) ]
        )

    /// Answers the base-table pool lookup with the pool root and every index query with an
    /// empty page, or fails index queries with `indexFailure`.
    let private client (indexFailure: exn option) =
        let mock = Mock<IAmazonDynamoDB>()

        mock
            .Setup(fun c -> c.QueryAsync(It.IsAny<QueryRequest>(), It.IsAny<CancellationToken>()))
            .Returns(fun (request: QueryRequest) (_: CancellationToken) ->
                match request.IndexName, indexFailure with
                | null, _ -> Task.FromResult(QueryResponse(Items = List [ poolRoot ]))
                | _, Some error -> Task.FromException<QueryResponse> error
                | _, None -> Task.FromResult(QueryResponse(Items = List())))
        |> ignore

        mock

    let private queries (mock: Mock<IAmazonDynamoDB>) =
        mock.Invocations
        |> Seq.filter (fun invocation -> invocation.Method.Name = "QueryAsync")
        |> Seq.map (fun invocation -> invocation.Arguments[0] :?> QueryRequest)
        |> List.ofSeq

    let private configure (mock: Mock<IAmazonDynamoDB>) (services: IServiceCollection) =
        services.AddLogging(fun builder ->
            builder.ClearProviders() |> ignore

            builder.AddProvider(
                { new ILoggerProvider with
                    member _.CreateLogger _ = NullLogger.Instance
                    member _.Dispose() = () }
            )
            |> ignore)
        |> ignore

        services.AddSingleton<IAmazonDynamoDB>(mock.Object) |> ignore
        registerAuthAsCaller services gamblerId

    let private request (path: (string * string) list) (next: string) =
        let request = APIGatewayHttpApiV2ProxyRequest()
        request.PathParameters <- Dictionary(dict path)
        request.QueryStringParameters <- Dictionary(dict [ "next", next ])
        attachJwtClaim request testEmail

    type private Endpoint =
        { Name: string
          Index: string
          Valid: string
          Invalid: (string * string) list
          Invoke: Mock<IAmazonDynamoDB> -> string -> Task<APIGatewayHttpApiV2ProxyResponse> }

    let private pool (handler: PoolFunction -> APIGatewayHttpApiV2ProxyRequest * TestLambdaContext -> _) path =
        fun mock next -> handler (PoolFunction(configure mock)) (request path next, TestLambdaContext())

    let private betPath = [ "poolId", poolId; "gamblerId", gamblerId ]

    let private malformed = "malformed", "not a token"
    let private unsupported = "unsupported version", """{"v":2,"key":{"pk":{"S":"x"}}}"""

    let private endpoints: Endpoint list =
        [ { Name = "leaderboard"
            Index = "GetPoolGamblerScoresByPool-index"
            Valid = scoreToken poolId gamblerId
            Invalid =
              [ malformed
                unsupported
                "legacy, missing score", legacy $"POOL#{poolId}" $"GAMBLER#{gamblerId}"
                "score as a string",
                token [ "pk", S $"POOL#{poolId}"; "sk", S $"GAMBLER#{gamblerId}"; "score", S "5" ]
                "another pool", scoreToken otherPoolId gamblerId ]
            Invoke = pool (fun f -> f.GetGamblersByPoolIdAsync) [ "poolId", poolId ] }
          { Name = "pending bets"
            Index = "GetPendingPoolGamblerBets-index"
            Valid = betToken poolId gamblerId "matchDateTime"
            Invalid =
              [ malformed
                "legacy, missing matchDateTime", legacy $"GAMBLER#{gamblerId}#POOL#{poolId}" $"MATCH#{matchId}"
                "another gambler", betToken poolId otherGamblerId "matchDateTime" ]
            Invoke = pool (fun f -> f.GetPendingBetsAsync) betPath }
          { Name = "live bets"
            Index = "GetPendingPoolGamblerBets-index"
            Valid = betToken poolId gamblerId "matchDateTime"
            Invalid =
              [ malformed
                "finished key", betToken poolId gamblerId "computedDateTime"
                "another pool", betToken otherPoolId gamblerId "matchDateTime" ]
            Invoke = pool (fun f -> f.GetLiveBetsAsync) betPath }
          { Name = "finished bets"
            Index = "GetFinishedPoolGamblerBetsByMatchDate-index"
            Valid = betToken poolId gamblerId "computedMatchDateTime"
            Invalid =
              [ malformed
                "legacy, missing computedMatchDateTime", legacy $"GAMBLER#{gamblerId}#POOL#{poolId}" $"MATCH#{matchId}"
                "computation-time index", betToken poolId gamblerId "computedDateTime"
                "another gambler", betToken poolId otherGamblerId "computedMatchDateTime" ]
            Invoke = pool (fun f -> f.GetFinishedBetsAsync) betPath }
          { Name = "timeline"
            Index = "GetFinishedPoolGamblerBetsByMatchDate-index"
            Valid = "finished:" + betToken poolId gamblerId "computedMatchDateTime"
            Invalid =
              [ "live, malformed", "live:not a token"
                "live, legacy", "live:" + legacy $"GAMBLER#{gamblerId}#POOL#{poolId}" $"MATCH#{matchId}"
                "finished, computation-time index", "finished:" + betToken poolId gamblerId "computedDateTime"
                "finished, another pool", "finished:" + betToken otherPoolId gamblerId "computedMatchDateTime"
                "unprefixed finished key", betToken poolId gamblerId "computedMatchDateTime" ]
            Invoke = pool (fun f -> f.GetBetsTimelineAsync) betPath }
          { Name = "legacy match bets"
            Index = "GetPoolGamblerScoresByMatch-index"
            Valid = matchBetToken matchId
            Invalid =
              [ malformed
                "legacy, missing index keys", legacy $"GAMBLER#{gamblerId}#POOL#{poolId}" $"MATCH#{matchId}"
                "another match", matchBetToken otherMatchId ]
            Invoke = pool (fun f -> f.GetMatchBetsAsync) [ "poolId", poolId; "matchId", matchId ] }
          { Name = "gambler pools"
            Index = "GetPoolGamblerScoresByGambler-index"
            Valid = scoreToken poolId gamblerId
            Invalid =
              [ malformed
                "legacy, missing score", legacy $"POOL#{poolId}" $"GAMBLER#{gamblerId}"
                "another gambler", scoreToken poolId otherGamblerId ]
            Invoke =
              fun mock next ->
                  GamblerFunction(configure mock)
                      .GetPoolsByGamblerIdAsync(request [ "gamblerId", gamblerId ] next, TestLambdaContext()) }
          { Name = "pool members"
            Index = "GetPoolGamblersByUsername-index"
            Valid = memberToken poolId
            Invalid =
              [ malformed
                "legacy, missing username", legacy $"POOL#{poolId}" $"GAMBLER#{gamblerId}"
                "another pool", memberToken otherPoolId ]
            Invoke =
              fun mock next ->
                  GamblerFunction(configure mock)
                      .GetPoolMembersAsync(request [ "poolId", poolId ] next, TestLambdaContext()) }
          { Name = "open layouts"
            Index = "GetOpenedPoolLayout-index"
            Valid = layoutToken "OPENED"
            Invalid =
              [ malformed
                "legacy, missing status and start date", legacy $"POOLLAYOUT#{layoutId}" $"POOLLAYOUT#{layoutId}"
                "closed status", layoutToken "CLOSED" ]
            Invoke =
              fun mock next ->
                  PoolLayoutFunction(configure mock).GetOpenPoolLayoutsAsync(request [] next, TestLambdaContext()) } ]

    let private endpoint name =
        endpoints |> List.find (fun endpoint -> endpoint.Name = name)

    let endpointNames: TheoryData<string> = TheoryData<string>(endpoints |> List.map _.Name)

    [<Theory>]
    [<MemberData(nameof endpointNames)>]
    let ``an unusable token is a 400 INVALID_CURSOR and the paginated query never runs`` (name: string) =
        let endpoint = endpoint name

        for case, next in endpoint.Invalid do
            let mock = client None
            let response = endpoint.Invoke mock next |> Async.AwaitTask |> Async.RunSynchronously

            (case, response.StatusCode) |> shouldEqual (case, int HttpStatusCode.BadRequest)

            response.Body
            |> shouldEqual
                """{"error":"INVALID_CURSOR","message":"The continuation is not valid; restart from the first page"}"""

            queries mock |> List.filter (fun query -> not (isNull query.IndexName)) |> shouldEqual []

    [<Theory>]
    [<MemberData(nameof endpointNames)>]
    let ``a usable token reaches the paginated query as its start key`` (name: string) =
        let endpoint = endpoint name
        let mock = client None

        let response =
            endpoint.Invoke mock endpoint.Valid |> Async.AwaitTask |> Async.RunSynchronously

        response.StatusCode |> shouldEqual (int HttpStatusCode.OK)

        let indexQueries = queries mock |> List.filter (fun query -> not (isNull query.IndexName))
        indexQueries |> List.map _.IndexName |> shouldEqual [ endpoint.Index ]

        let inner = endpoint.Valid.Substring(endpoint.Valid.IndexOf '{')
        let expected = (DynamoDbKeySerializer() :> IKeySerializer).Deserialize inner

        indexQueries.Head.ExclusiveStartKey
        |> Seq.map (fun entry -> entry.Key, entry.Value.S, entry.Value.N)
        |> Seq.sort
        |> List.ofSeq
        |> shouldEqual (
            expected
            |> Seq.map (fun entry -> entry.Key, entry.Value.S, entry.Value.N)
            |> Seq.sort
            |> List.ofSeq
        )

    [<Theory>]
    [<MemberData(nameof endpointNames)>]
    let ``a DynamoDB failure for a usable token keeps its existing handling`` (name: string) =
        let endpoint = endpoint name
        let mock = client (Some(AmazonDynamoDBException "Throughput exceeds the current capacity"))

        let outcome =
            endpoint.Invoke mock endpoint.Valid |> Async.AwaitTask |> Async.Catch |> Async.RunSynchronously

        match outcome with
        | Choice1Of2 response -> failwith $"expected the handler to fail, got {response.StatusCode} {response.Body}"
        | Choice2Of2 error -> error.GetBaseException() |> should be ofExactType<AmazonDynamoDBException>
