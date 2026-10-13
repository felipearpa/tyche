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
open Felipearpa.Core.Paging
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.AmazonLambda.Function
open Felipearpa.Tyche.AmazonLambda.Function.Tests.AuthorizationTestHelpers
open Felipearpa.Tyche.Function.Response
open Felipearpa.Tyche.Pool.Test.Fakes
open FsUnitTyped
open Microsoft.Extensions.DependencyInjection
open Microsoft.Extensions.Logging
open Microsoft.Extensions.Logging.Abstractions
open Xunit

/// History (GET /pools/{poolId}/gamblers/{gamblerId}/bets/finished) and Timeline
/// (GET .../bets/timeline) serve whole kickoff groups with layout counts and movement from the
/// stored snapshots, over the in-memory table with forced storage pages.
module GetFinishedBetsTest =

    let private poolId = "01K1PX1TX2NM1HG851S1V0P001"
    let private layoutId = "01K1PX1TX2NM1HG851S1V0Y001"
    let private gamblerId = "01K1PX1TX2NM1HG851S1V0GA01"
    let private kickoff = DateTime(2026, 6, 12, 17, 0, 0, DateTimeKind.Utc)
    let private matchOf (number: int) = $"01K1PX1TX2NM1HG851S1V0M{number:D3}"
    let private iso (value: DateTime) = value.ToString("o")
    let private s (value: string) = AttributeValue(S = value)
    let private n (value: int) = AttributeValue(N = string value)

    let private layoutMatch (number: int) (matchKickoff: DateTime) (hasResult: bool) =
        let item =
            Dictionary(
                dict
                    [ "pk", s $"POOLLAYOUT#{layoutId}"
                      "sk", s $"MATCH#{matchOf number}"
                      "matchId", s (matchOf number)
                      "poolLayoutId", s layoutId
                      "homeTeamId", s "COL"
                      "homeTeamName", s "Colombia"
                      "awayTeamId", s "ARG"
                      "awayTeamName", s "Argentina"
                      "matchDateTime", s (iso matchKickoff)
                      "poolLayoutVersion", n 1
                      "round", s "Fase de grupos" ]
            )

        if hasResult then
            item["homeTeamScore"] <- n 2
            item["awayTeamScore"] <- n 1

        item

    /// A detail; a computed one carries its computation hour and optional snapshot.
    let private detail (number: int) (matchKickoff: DateTime) (computed: (float * (int * int) option) option) =
        let item =
            Dictionary(
                dict
                    [ "pk", s $"GAMBLER#{gamblerId}#POOL#{poolId}"
                      "sk", s $"MATCH#{matchOf number}"
                      "poolId", s poolId
                      "gamblerId", s gamblerId
                      "gamblerUsername", s "ana"
                      "matchId", s (matchOf number)
                      "poolLayoutId", s layoutId
                      "homeTeamId", s "COL"
                      "homeTeamName", s "Colombia"
                      "awayTeamId", s "ARG"
                      "awayTeamName", s "Argentina"
                      "homeTeamBet", n 2
                      "awayTeamBet", n 1
                      "matchDateTime", s (iso matchKickoff) ]
            )

        computed
        |> Option.iter (fun (hours, snapshot) ->
            item["homeTeamScore"] <- n 2
            item["awayTeamScore"] <- n 1
            item["score"] <- n 3
            item["computedRequestId"] <- s "01K1PX1TX2NM1HG851S1V0R000"
            item["computedDateTime"] <- s (iso (kickoff.AddHours hours))
            item["computedMatchDateTime"] <- s (iso matchKickoff)

            snapshot
            |> Option.iter (fun (before, after) ->
                item["beforePosition"] <- n before
                item["position"] <- n after))

        item

    let private fourMatches = kickoff.AddDays 2.0
    let private partial = kickoff.AddDays 1.0

    /// A four-match group settled 8 → 5 → 6 → 4 → 3; a two-match group with one result
    /// (7 → 2); and an older group whose only child predates position snapshots.
    let private seed () =
        let fake = FakeTables.create ()

        let fixtures =
            [ 101, fourMatches, true, Some(50.0, Some(8, 5))
              102, fourMatches, true, Some(51.0, Some(5, 6))
              103, fourMatches, true, Some(52.0, Some(6, 4))
              104, fourMatches, true, Some(53.0, Some(4, 3))
              201, partial, true, Some(30.0, Some(7, 2))
              202, partial, false, None
              301, kickoff, true, Some(2.0, None) ]

        for number, matchKickoff, hasResult, computed in fixtures do
            fake.Put("PoolLayout", layoutMatch number matchKickoff hasResult)
            fake.Put("Pool", detail number matchKickoff computed)

        fake.LimitQueryPages 3
        fake

    let private functions (client: IAmazonDynamoDB) =
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

            services.AddSingleton<IAmazonDynamoDB>(client) |> ignore
            registerAuthAsCaller services gamblerId)

    let private request (next: string option) =
        let request = APIGatewayHttpApiV2ProxyRequest()
        request.PathParameters <- dict [ "poolId", poolId; "gamblerId", gamblerId ]
        next |> Option.iter (fun token -> request.QueryStringParameters <- dict [ "next", token ])
        attachJwtClaim request testEmail |> ignore
        request

    let private history (functions: PoolFunction) next =
        functions.GetFinishedBetsAsync(request next, TestLambdaContext()) |> Async.AwaitTask

    let private timeline (functions: PoolFunction) next =
        functions.GetBetsTimelineAsync(request next, TestLambdaContext()) |> Async.AwaitTask

    let private read (response: APIGatewayHttpApiV2ProxyResponse) =
        response.StatusCode |> shouldEqual (int HttpStatusCode.OK)
        (JsonSerializer() :> ISerializer).Deserialize<CursorPage<KickoffGroupResponse>>(response.Body)

    let private traverse (fetch: string option -> Async<APIGatewayHttpApiV2ProxyResponse>) =
        let rec loop next pages =
            async {
                let! response = fetch next
                let page = read response
                let pages = pages @ [ List.ofSeq page.Items, page.Next ]

                match page.Next with
                | Some _ when pages.Length < 20 -> return! loop page.Next pages
                | _ -> return pages
            }

        loop None []

    let private describe (group: KickoffGroupResponse) =
        group.KickoffDateTime,
        group.Matches |> List.map (fun row -> row.MatchId),
        (group.ComputedMatches, group.TotalMatches),
        group.Movement |> Option.map (fun movement -> movement.BeforePosition, movement.Position)

    let private fourMatchGroup =
        fourMatches, [ matchOf 101; matchOf 102; matchOf 103; matchOf 104 ], (4, 4), Some(8, 3)

    let private partialInHistory = partial, [ matchOf 201 ], (1, 2), Some(7, 2)
    let private legacyGroup = kickoff, [ matchOf 301 ], (1, 1), None

    [<Fact>]
    let ``History returns whole groups across storage pages with counts and movement`` () =
        async {
            let functions = functions (seed ()).Client
            let! pages = traverse (history functions)

            // The four-match group is cut by the first storage page and completed.
            pages
            |> List.map (fst >> List.map describe)
            |> shouldEqual [ [ fourMatchGroup ]; [ partialInHistory; legacyGroup ] ]

            pages |> List.last |> snd |> shouldEqual None
        }

    [<Fact>]
    let ``History serializes a missing snapshot as a null movement and keeps the rows`` () =
        async {
            let fake = seed ()
            fake.LimitQueryPages 10
            let! response = history (functions fake.Client) None

            response.Body |> shouldContainText "\"kickoffDateTime\":\"2026-06-12T17:00:00Z\""
            response.Body |> shouldContainText "\"movement\":null"

            let legacy = (read response).Items |> Seq.last
            legacy.Matches |> List.map (fun row -> row.Score, row.HomeTeamScore, row.HomeTeamBet) |> shouldEqual [ Some 3, Some 2, Some 2 ]
        }

    [<Fact>]
    let ``History rejects a token of the computation-time index with INVALID_CURSOR`` () =
        async {
            let oldToken =
                match
                    ContinuationKey.tryCreate (
                        Map
                            [ "pk", S $"GAMBLER#{gamblerId}#POOL#{poolId}"
                              "sk", S $"MATCH#{matchOf 101}"
                              "computedDateTime", S (iso kickoff) ]
                    )
                with
                | Ok key -> ContinuationToken.encode key
                | Error error -> failwith $"invalid test key: %A{error}"

            let fake = seed ()
            let! response = history (functions fake.Client) (Some oldToken)

            response.StatusCode |> shouldEqual (int HttpStatusCode.BadRequest)

            response.Body
            |> shouldEqual """{"error":"INVALID_CURSOR","message":"The continuation is not valid; restart from the first page"}"""

            // A fresh request returns the grouped contract.
            let! fresh = history (functions fake.Client) None
            (read fresh).Items |> Seq.head |> describe |> shouldEqual fourMatchGroup
        }

    [<Fact>]
    let ``Timeline shows a partial group once, unsettled first, with every revealed row`` () =
        async {
            let functions = functions (seed ()).Client
            let! pages = traverse (timeline functions)

            pages
            |> List.collect fst
            |> List.map describe
            |> shouldEqual
                [ partial, [ matchOf 201; matchOf 202 ], (1, 2), Some(7, 2)
                  fourMatchGroup
                  legacyGroup ]

            let pending = pages |> List.collect fst |> List.head |> _.Matches |> List.last
            (pending.isComputed, pending.Score) |> shouldEqual (false, None)
        }

    [<Fact>]
    let ``given a request without path parameters when History is requested then bad response is returned`` () =
        async {
            let! response = PoolFunction().GetFinishedBetsAsync(APIGatewayHttpApiV2ProxyRequest(), TestLambdaContext()) |> Async.AwaitTask
            response.StatusCode |> shouldEqual (int HttpStatusCode.BadRequest)
        }
