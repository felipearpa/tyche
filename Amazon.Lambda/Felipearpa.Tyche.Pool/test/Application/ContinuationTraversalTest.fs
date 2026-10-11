namespace Felipearpa.Tyche.Pool.Test.Application

#nowarn "3536"

open System
open System.Collections.Generic
open Amazon.DynamoDBv2
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.Pool.Application
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Infrastructure
open Felipearpa.Tyche.Pool.Test.Fakes
open Felipearpa.Tyche.Pool.Test.Infrastructure.PaginationSupport
open Felipearpa.Tyche.Pool.Test.Settlement
open Felipearpa.Tyche.Pool.Test.Settlement.PoolScenario
open Felipearpa.Type
open FsUnitTyped
open Xunit

/// Application consumers of shared-codec continuations across forced storage pages: the
/// Timeline phases, added-match fan-out, and joined-gambler layout-match traversal.
module ContinuationTraversalTest =

    let private kickoff = DateTime(2026, 6, 12, 17, 0, 0, DateTimeKind.Utc)
    let private iso (value: DateTime) = value.ToString("o")
    let private matchOf (number: int) = $"01K1PX1TX2NM1HG851S1V0B{number:D3}"

    let private timelineBet (number: int) (computedHours: float option) =
        let computed =
            match computedHours with
            | Some hours ->
                [ "computedRequestId", s "01K1PX1TX2NM1HG851S1V0R000"
                  "computedDateTime", s (iso (kickoff.AddHours hours))
                  "score", n 3 ]
            | None -> []

        Dictionary(
            dict (
                [ "pk", s $"GAMBLER#{gamblerA}#POOL#{poolId}"
                  "sk", s $"MATCH#{matchOf number}"
                  "poolId", s poolId
                  "gamblerId", s gamblerA
                  "gamblerUsername", s "ana"
                  "gamblerEmail", s "ana@example.com"
                  "matchId", s (matchOf number)
                  "poolLayoutId", s poolLayoutId
                  "homeTeamId", s "COL"
                  "homeTeamName", s "Colombia"
                  "awayTeamId", s "ARG"
                  "awayTeamName", s "Argentina"
                  "matchDateTime", s (iso (kickoff.AddHours(float number))) ]
                @ computed
            )
        )

    /// Live (newest match first): B004 alone on the first storage page, an empty page of
    /// computed B003/B002, then B001. Finished (newest settlement first): B005, B003, B002.
    let private timeline () =
        let fake = FakeTables.create ()

        [ timelineBet 1 None
          timelineBet 2 (Some 10.0)
          timelineBet 3 (Some 11.0)
          timelineBet 4 None
          timelineBet 5 (Some 12.0) ]
        |> List.iter (fun item -> fake.Put("Pool", item))

        fake.LimitQueryPages 2
        let bets = PoolGamblerBetDynamoDbRepository(typed, fake.Client)
        let useCase = GetGamblerBetsTimeline(GetLivePoolGamblerBets(bets), GetFinishedPoolGamblerBets(bets))
        fun cursor -> useCase.ExecuteAsync(Ulid.newOf poolId, Ulid.newOf gamblerA, cursor)

    let private ids (page: Felipearpa.Core.Paging.CursorPage<PoolGamblerBet>) =
        page.Items |> Seq.map (fun bet -> bet.MatchId.Value.Substring 22) |> List.ofSeq

    let private inner (prefix: string) (cursor: string option) =
        match cursor with
        | Some value when value.StartsWith prefix -> value.Substring prefix.Length
        | other -> failwith $"expected a '{prefix}' cursor, got %A{other}"

    [<Fact>]
    let ``given live entries before finished ones when the timeline is followed then it stays live until live is exhausted`` () =
        async {
            let fetch = timeline ()

            let! first = fetch None
            let! emptyLive = fetch first.Next
            let! boundary = fetch emptyLive.Next
            let! last = fetch boundary.Next

            [ ids first; ids emptyLive; ids boundary; ids last ]
            |> shouldEqual [ [ "B004" ]; []; [ "B001"; "B005"; "B003" ]; [ "B002" ] ]

            inner "live:" first.Next |> ignore
            inner "live:" emptyLive.Next |> ignore
            last.Next |> shouldEqual None

            inner "finished:" boundary.Next
            |> typed.Deserialize
            |> describeKey
            |> shouldEqual (
                Map
                    [ "pk", ("S", $"GAMBLER#{gamblerA}#POOL#{poolId}")
                      "sk", ("S", $"MATCH#{matchOf 3}")
                      "computedDateTime", ("S", iso (kickoff.AddHours 11.0)) ]
            )
        }

    [<Fact>]
    let ``given an unprefixed live token when the timeline continues then it resumes the live phase`` () =
        async {
            let fetch = timeline ()
            let! first = fetch None
            let! prefixed = fetch first.Next

            let! unprefixed = fetch (Some(inner "live:" first.Next))

            ids unprefixed |> shouldEqual (ids prefixed)
            unprefixed.Next |> shouldEqual prefixed.Next
        }

    [<Fact>]
    let ``given an unprefixed finished token when the timeline continues then it is read as live and rejected`` () =
        async {
            let fetch = timeline ()
            let! first = fetch None
            let! second = fetch first.Next
            let! boundary = fetch second.Next

            let! reason = invalidContinuationOf (fetch (Some(inner "finished:" boundary.Next)))

            reason |> shouldEqual (MissingKeyAttribute "matchDateTime")
        }

    let private addedMatch result =
        { MatchId = match3
          MatchDateTime = pastKickoff.AddDays(1.0)
          Result = result }

    let private migrated (serializer: IKeySerializer) =
        async {
            let harness =
                VersionedSettlementHarness(
                    { HarnessOptions.Default with
                        KeySerializer = serializer }
                )

            do! harness.SeedAndMigrateAsync(defaultMembers, defaultMatches)
            harness.Fake.LimitQueryPages 1
            harness.Fake.TakeQueries() |> ignore
            return harness
        }

    let private failLaterPages (indexOrTable: string) (request: obj) =
        match request with
        | :? QueryRequest as query when
            (query.IndexName = indexOrTable || (isNull query.IndexName && query.TableName = indexOrTable))
            && not (isNull query.ExclusiveStartKey)
            && query.ExclusiveStartKey.Count > 0
            ->
            FailBefore(AmazonDynamoDBException "Throughput exceeded")
        | _ -> Proceed

    let private reconciliation = $"{poolId}#MATCH#{match3}#INSERT#{match3}"

    [<Fact>]
    let ``given members on separate gambler pages when an added match is fanned out then every page is reached`` () =
        async {
            let! harness = migrated typed

            do! harness.HandleLayoutRecordsAsync [ MatchInserted(addedMatch (Some(1, 1))) ]

            for gamblerId in [ gamblerA; gamblerB; gamblerC ] do
                detail harness.Fake gamblerId match3 |> Option.isSome |> shouldEqual true

            harness.Commands.Pending |> List.map _.DeduplicationId |> shouldEqual [ reconciliation ]

            harness.Fake.TakeQueries()
            |> List.filter (fun query -> query.IndexName = PoolTable.Index.gamblersByPoolLayout)
            |> List.map (fun query ->
                query.ExclusiveStartKey
                |> Option.ofObj
                |> Option.filter (fun key -> key.Count > 0)
                |> Option.map (fun key -> key["getGamblersByPoolLayoutSk"].S))
            |> shouldEqual
                [ None
                  Some $"POOL#{poolId}#GAMBLER#{gamblerA}"
                  Some $"POOL#{poolId}#GAMBLER#{gamblerB}" ]
        }

    [<Fact>]
    let ``given a later gambler page fails when an added match is fanned out then the work fails and redelivery completes it`` () =
        async {
            let! harness = migrated typed
            harness.Fake.Intercept(failLaterPages PoolTable.Index.gamblersByPoolLayout)

            let! _ = failureOf (harness.HandleLayoutRecordsAsync [ MatchInserted(addedMatch (Some(1, 1))) ])

            detail harness.Fake gamblerA match3 |> Option.isSome |> shouldEqual true
            detail harness.Fake gamblerC match3 |> shouldEqual None
            harness.Commands.Pending |> shouldEqual []

            harness.Fake.ClearInterception()
            do! harness.HandleLayoutRecordsAsync [ MatchInserted(addedMatch (Some(1, 1))) ]

            for gamblerId in [ gamblerA; gamblerB; gamblerC ] do
                detail harness.Fake gamblerId match3 |> Option.isSome |> shouldEqual true

            harness.Commands.Pending |> List.map _.DeduplicationId |> shouldEqual [ reconciliation ]
        }

    [<Fact>]
    let ``given a pk and sk only writer when fan-out crosses a gambler page then it fails instead of completing`` () =
        async {
            let! harness = migrated legacyTokens

            let! reason = invalidContinuationOf (harness.HandleLayoutRecordsAsync [ MatchInserted(addedMatch None) ])

            reason |> shouldEqual (MissingKeyAttribute "getGamblersByPoolLayoutPk")
            detail harness.Fake gamblerC match3 |> shouldEqual None
        }

    let private joinedD (harness: VersionedSettlementHarness) =
        async {
            let! joined = harness.JoinAsync gamblerD
            joined |> shouldEqual (Ok())
            harness.Fake.TakeChanges() |> ignore

            let materializer =
                MaterializeJoinedGambler(
                    harness.Bets,
                    harness.Projection,
                    harness.LayoutReader,
                    harness.CommandPublisher,
                    fun () -> harness.Now()
                )

            let gambler =
                { PoolLayoutGambler.PoolId = Ulid.newOf poolId
                  PoolLayoutId = Ulid.newOf poolLayoutId
                  PoolLayoutVersion = 1
                  GamblerId = Ulid.newOf gamblerD
                  GamblerUsername = NonEmptyString100.newOf $"user-{gamblerD.Substring 22}"
                  GamblerEmail = NonEmptyString100.newOf $"{gamblerD.Substring 22}@example.com" }

            return fun () -> materializer.ExecuteAsync(gambler, "EVENT1")
        }

    let serializers: obj array seq = [ [| box "version 1" |]; [| box "legacy pk/sk" |] ]

    [<Theory; MemberData(nameof serializers)>]
    let ``given layout matches on separate pages when a joined gambler is materialized then every match is reached``
        (writer: string)
        =
        async {
            let! harness = migrated (if writer = "version 1" then typed else legacyTokens)
            let! materialize = joinedD harness

            do! materialize ()

            for matchId in [ match0; match1; match2 ] do
                detail harness.Fake gamblerD matchId |> Option.isSome |> shouldEqual true
        }

    [<Fact>]
    let ``given a later layout-match page fails when a joined gambler is materialized then the work fails and a retry completes it`` () =
        async {
            let! harness = migrated typed
            let! materialize = joinedD harness
            harness.Fake.Intercept(failLaterPages "PoolLayout")

            let! _ = failureOf (materialize ())

            detail harness.Fake gamblerD match0 |> Option.isSome |> shouldEqual true
            detail harness.Fake gamblerD match2 |> shouldEqual None

            harness.Fake.ClearInterception()
            do! materialize ()

            for matchId in [ match0; match1; match2 ] do
                detail harness.Fake gamblerD matchId |> Option.isSome |> shouldEqual true
        }
