namespace Felipearpa.Tyche.Pool.Test.Application

#nowarn "3536"

open System
open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Core.Paging
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.Pool.Application
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Infrastructure
open Felipearpa.Tyche.Pool.Test.Fakes
open Felipearpa.Tyche.Pool.Test.Infrastructure.PaginationSupport
open Felipearpa.Tyche.PoolLayout.Application
open Felipearpa.Tyche.PoolLayout.Infrastructure
open Felipearpa.Type
open FsUnitTyped
open Xunit

/// The grouped History and Timeline feeds over forced storage pages: whole kickoff groups per
/// page, layout counts, movement from stored snapshots, group-level Timeline phases, and the
/// rejection of continuations issued for the computation-time index.
module GroupedBetFeedTest =

    let private poolId = "01K1PX1TX2NM1HG851S1V0P001"
    let private layoutId = "01K1PX1TX2NM1HG851S1V0Y001"
    let private gamblerId = "01K1PX1TX2NM1HG851S1V0GA01"
    let private kickoff = DateTime(2026, 6, 12, 17, 0, 0, DateTimeKind.Utc)
    let private iso (value: DateTime) = value.ToUniversalTime().ToString("o")
    let private matchOf (number: int) = $"01K1PX1TX2NM1HG851S1V0M{number:D3}"

    /// A settled detail's computation time and its stored position snapshot.
    type private Computed =
        { At: DateTime
          Before: int option
          After: int option }

    let private computedAt (hours: float) before after =
        Some
            { At = kickoff.AddHours hours
              Before = before
              After = after }

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
            item["homeTeamScore"] <- n 1
            item["awayTeamScore"] <- n 0
            item["resultVersion"] <- n 1

        item

    let private detail (number: int) (matchKickoff: DateTime) (computed: Computed option) =
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
                      "homeTeamBet", n 1
                      "awayTeamBet", n 0
                      "matchDateTime", s (iso matchKickoff) ]
            )

        computed
        |> Option.iter (fun computed ->
            item["homeTeamScore"] <- n 1
            item["awayTeamScore"] <- n 0
            item["score"] <- n 5
            item["computedRequestId"] <- s "01K1PX1TX2NM1HG851S1V0R000"
            item["computedDateTime"] <- s (iso computed.At)
            item["computedMatchDateTime"] <- s (iso matchKickoff)
            computed.Before |> Option.iter (fun position -> item["beforePosition"] <- n position)
            computed.After |> Option.iter (fun position -> item["position"] <- n position))

        item

    /// One match of the layout and the gambler's detail for it.
    type private Fixture =
        { Number: int
          Kickoff: DateTime
          HasResult: bool
          Computed: Computed option }

    let private fixtureOf (number, matchKickoff, hasResult, computed) =
        { Number = number
          Kickoff = matchKickoff
          HasResult = hasResult
          Computed = computed }

    let private seed (pageSize: int) (fixtures: Fixture list) =
        let fake = FakeTables.create ()

        for fixture in fixtures do
            fake.Put("PoolLayout", layoutMatch fixture.Number fixture.Kickoff fixture.HasResult)
            fake.Put("Pool", detail fixture.Number fixture.Kickoff fixture.Computed)

        fake.LimitQueryPages pageSize
        fake

    let private services (fake: FakeDynamoDb) =
        let bets = PoolGamblerBetDynamoDbRepository(typed, fake.Client)
        let layouts = PoolLayoutDynamoDbRepository(typed, fake.Client)
        let reader = KickoffGroupReader(PoolLayoutMatchReader(layouts))
        GetFinishedPoolGamblerBets(bets, reader), GetGamblerBetsTimeline(bets, reader)

    let private history (fake: FakeDynamoDb) =
        let history, _ = services fake
        fun next -> history.ExecuteAsync(Ulid.newOf poolId, Ulid.newOf gamblerId, next)

    let private timeline (fake: FakeDynamoDb) =
        let _, timeline = services fake
        fun next -> timeline.ExecuteAsync(Ulid.newOf poolId, Ulid.newOf gamblerId, next)

    /// A group as the tests compare it: kickoff, match numbers, computed/total, movement.
    let private describe (item: KickoffGroupItem) =
        item.Group.KickoffDateTime,
        item.Bets |> List.map (fun bet -> int (bet.MatchId.Value.Substring 23)),
        (item.Group.ComputedMatches, item.Group.TotalMatches),
        item.Movement |> Option.map (fun movement -> movement.BeforePosition, movement.Position)

    let private pagesOf (pages: (KickoffGroupItem list * string option) list) =
        pages |> List.map (fun (items, _) -> items |> List.map describe)

    let private movement before after = Some(before, after)

    let private newer = kickoff.AddDays 1.0

    /// A four-match group whose children settled 8 → 5 → 6 → 4 → 3, and an older one-match group.
    let private fourMatchGroup =
        [ 101, newer, true, computedAt 30.0 (Some 8) (Some 5)
          102, newer, true, computedAt 31.0 (Some 5) (Some 6)
          103, newer, true, computedAt 32.0 (Some 6) (Some 4)
          104, newer, true, computedAt 33.0 (Some 4) (Some 3)
          1, kickoff, true, computedAt 2.0 (Some 10) (Some 8) ]
        |> List.map fixtureOf

    [<Fact>]
    let ``given a group cut by a storage page when History is read then the page completes the group`` () =
        async {
            let fake = seed 3 fourMatchGroup
            let! pages = traverse (history fake)

            pagesOf pages
            |> shouldEqual
                [ [ newer, [ 101; 102; 103; 104 ], (4, 4), movement 8 3 ]
                  [ kickoff, [ 1 ], (1, 1), movement 10 8 ] ]

            // The continuation resumes after the group's last child in index order.
            pages.Head
            |> snd
            |> Option.get
            |> typed.Deserialize
            |> describeKey
            |> shouldEqual (
                Map
                    [ "pk", ("S", $"GAMBLER#{gamblerId}#POOL#{poolId}")
                      "sk", ("S", $"MATCH#{matchOf 101}")
                      "computedMatchDateTime", ("S", iso newer) ]
            )
        }

    [<Fact>]
    let ``given a group that fits its storage page when History is read then no completion query runs`` () =
        async {
            let fake = seed 10 fourMatchGroup
            let! page = history fake None

            page.Items |> List.ofSeq |> List.map describe |> List.length |> shouldEqual 2
            page.Next |> shouldEqual None

            fake.TakeQueries()
            |> List.filter (fun query -> query.IndexName = PoolTable.Index.finishedPoolGamblerBetsByMatchDate)
            |> List.length
            |> shouldEqual 1
        }

    [<Fact>]
    let ``given a partial group when History is read then it carries the layout counts and only computed rows`` () =
        async {
            let fake =
                seed
                    10
                    ([ 201, newer, true, computedAt 30.0 (Some 7) (Some 2)
                       202, newer, false, None
                       203, newer, false, None ]
                     |> List.map fixtureOf)

            let! page = history fake None

            page.Items |> List.ofSeq |> List.map describe |> shouldEqual [ newer, [ 201 ], (1, 3), movement 7 2 ]
        }

    [<Fact>]
    let ``given a legacy child without snapshots when History is read then the movement is null and the rows remain`` () =
        async {
            let fake =
                seed
                    10
                    ([ 301, newer, true, computedAt 30.0 None None
                       302, newer, true, computedAt 31.0 (Some 4) (Some 2)
                       1, kickoff, true, computedAt 2.0 (Some 3) None ]
                     |> List.map fixtureOf)

            let! page = history fake None
            let items = List.ofSeq page.Items

            items
            |> List.map describe
            |> shouldEqual [ newer, [ 301; 302 ], (2, 2), None; kickoff, [ 1 ], (1, 1), None ]

            items.Head.Bets |> List.map _.Score |> shouldEqual [ Some 5; Some 5 ]
        }

    [<Fact>]
    let ``given an older kickoff computed after a newer one when History is read then kickoff order wins`` () =
        async {
            let sameDay = kickoff.AddHours 3.0

            let fake =
                seed
                    10
                    ([ 401, kickoff, true, computedAt 50.0 (Some 2) (Some 1)
                       403, sameDay, true, computedAt 5.0 (Some 1) (Some 2)
                       402, sameDay, true, computedAt 6.0 (Some 2) (Some 2) ]
                     |> List.map fixtureOf)

            let! page = history fake None

            page.Items
            |> List.ofSeq
            |> List.map describe
            |> shouldEqual [ sameDay, [ 402; 403 ], (2, 2), movement 1 2; kickoff, [ 401 ], (1, 1), movement 2 1 ]
        }

    let private oldFinishedToken =
        tokenOf
            [ "pk", s $"GAMBLER#{gamblerId}#POOL#{poolId}"
              "sk", s $"MATCH#{matchOf 1}"
              "computedDateTime", s (iso kickoff) ]

    [<Fact>]
    let ``given a token of the computation-time index when History or Timeline continue then it is rejected`` () =
        async {
            let fake = seed 10 fourMatchGroup

            let! fromHistory = invalidContinuationOf (history fake (Some oldFinishedToken))
            let! fromTimeline = invalidContinuationOf (timeline fake (Some("finished:" + oldFinishedToken)))

            fromHistory |> shouldEqual (MissingKeyAttribute "computedMatchDateTime")
            fromTimeline |> shouldEqual (MissingKeyAttribute "computedMatchDateTime")

            fake.TakeQueries()
            |> List.filter (fun query -> not (isNull query.IndexName))
            |> shouldEqual []
        }

    [<Fact>]
    let ``given a History token issued for another gambler when it is reused then it is rejected`` () =
        async {
            let fake = seed 3 fourMatchGroup
            let! first = history fake None

            let foreign =
                first.Next
                |> Option.get
                |> fun token -> token.Replace(gamblerId, "01K1PX1TX2NM1HG851S1V0GB01")

            let! reason = invalidContinuationOf (history fake (Some foreign))
            reason |> shouldEqual (KeyOutOfScope("pk", "the value belongs to another partition or key"))
        }

    let private future = DateTime.UtcNow.AddDays 2.0

    /// A settled one-match group, a two-match group with one result, a pending group whose
    /// predictions are revealed, and a future group whose predictions are still hidden.
    let private timelineFixtures =
        [ 1, kickoff, true, computedAt 2.0 (Some 10) (Some 8)
          501, newer, true, computedAt 30.0 (Some 8) (Some 6)
          502, newer, false, None
          601, kickoff.AddDays 2.0, false, None
          701, future, false, None ]
        |> List.map fixtureOf

    let private partial = newer, [ 501; 502 ], (1, 2), movement 8 6
    let private pending = kickoff.AddDays 2.0, [ 601 ], (0, 1), None
    let private settled = kickoff, [ 1 ], (1, 1), movement 10 8

    let private phaseOf (token: string option) =
        token |> Option.map (fun value -> value.Substring(0, value.IndexOf ':'))

    [<Fact>]
    let ``given a partial group when Timeline is traversed then it appears once in the unsettled phase`` () =
        async {
            let fake = seed 2 timelineFixtures
            let! pages = traverse (timeline fake)

            // The first unsettled storage page holds only computed details and is filtered empty,
            // yet the phase continues; the last unsettled page moves on to the settled phase.
            pagesOf pages |> shouldEqual [ []; [ partial; pending; settled ] ]
            pages |> List.map (snd >> phaseOf) |> shouldEqual [ Some "live"; None ]
        }

    [<Fact>]
    let ``given several unsettled storage pages when Timeline is traversed then it stays unsettled until they end`` () =
        async {
            let fake = seed 1 timelineFixtures
            let! pages = traverse (timeline fake)

            let groups = pages |> List.collect fst |> List.map describe
            groups |> shouldEqual [ partial; pending; settled ]

            let phases = pages |> List.map (snd >> phaseOf)
            let lastLive = phases |> List.findIndexBack ((=) (Some "live"))
            let firstFinished = phases |> List.findIndex ((=) (Some "finished"))
            lastLive < firstFinished |> shouldEqual true

            // Empty pages kept their phase rather than ending it.
            pages |> List.exists (fun (items, next) -> items.IsEmpty && phaseOf next = Some "live") |> shouldEqual true
        }

    [<Fact>]
    let ``given the last result of a partial group when Timeline is refreshed then the group moves to the settled phase`` () =
        async {
            let fake = seed 2 timelineFixtures

            fake.Put("PoolLayout", layoutMatch 502 newer true)
            fake.Put("Pool", detail 502 newer (computedAt 31.0 (Some 6) (Some 5)))

            let! pages = traverse (timeline fake)

            pages
            |> List.collect fst
            |> List.map describe
            |> shouldEqual [ pending; newer, [ 501; 502 ], (2, 2), movement 8 5; settled ]
        }

    [<Fact>]
    let ``given hidden predictions when Timeline is read then their group is not shown and counts reveal nothing`` () =
        async {
            let fake = seed 10 timelineFixtures
            let! pages = traverse (timeline fake)
            let groups = pages |> List.collect fst

            groups |> List.exists (fun item -> item.Group.KickoffDateTime = KickoffGroup.instant future) |> shouldEqual false
            groups |> List.collect _.Bets |> List.map (fun bet -> bet.MatchId.Value) |> shouldNotContain (matchOf 701)
        }

    [<Fact>]
    let ``given an unprefixed unsettled token when Timeline continues then it resumes the unsettled phase`` () =
        async {
            let fake = seed 1 timelineFixtures
            let! first = timeline fake None
            let inner = first.Next.Value.Substring "live:".Length

            let! prefixed = timeline fake first.Next
            let! unprefixed = timeline fake (Some inner)

            unprefixed.Items |> List.ofSeq |> List.map describe |> shouldEqual (prefixed.Items |> List.ofSeq |> List.map describe)
            unprefixed.Next |> shouldEqual prefixed.Next
        }

    [<Fact>]
    let ``a group's movement spans its earliest and latest computed children and never sums them`` () =
        let bet number (computedHours: float option) before after =
            { Bet =
                { PoolId = Ulid.newOf poolId
                  GamblerId = Ulid.newOf gamblerId
                  GamblerUsername = Felipearpa.Type.NonEmptyString100.newOf "ana"
                  MatchId = Ulid.newOf (matchOf number)
                  PoolLayoutId = Ulid.newOf layoutId
                  HomeTeamId = "COL"
                  HomeTeamName = Felipearpa.Type.NonEmptyString100.newOf "Colombia"
                  AwayTeamId = "ARG"
                  AwayTeamName = Felipearpa.Type.NonEmptyString100.newOf "Argentina"
                  MatchScore = None
                  BetScore = None
                  Score = None
                  MatchDateTime = kickoff
                  Round = "Fase de grupos"
                  GroupName = None
                  isLocked = true
                  isComputed = computedHours.IsSome }
              Kickoff = kickoff
              StoredKickoff = iso kickoff
              ComputedDateTime = computedHours |> Option.map kickoff.AddHours
              BeforePosition = before
              Position = after }

        // Settled in the order 8 → 5, then 5 → 6; listed out of order.
        KickoffGroupFeed.movement [ bet 2 (Some 3.0) (Some 5) (Some 6); bet 1 (Some 2.0) (Some 8) (Some 5) ]
        |> shouldEqual (Some { BeforePosition = 8; Position = 6 })

        // Ties in computation time fall back to match ID order.
        KickoffGroupFeed.movement [ bet 2 (Some 2.0) (Some 5) (Some 6); bet 1 (Some 2.0) (Some 8) (Some 5) ]
        |> shouldEqual (Some { BeforePosition = 8; Position = 6 })

        // A pending child does not count; no computed child means no movement.
        KickoffGroupFeed.movement [ bet 1 None (Some 1) (Some 1) ] |> shouldEqual None
        KickoffGroupFeed.movement [ bet 1 (Some 2.0) (Some 8) (Some 5); bet 2 None None None ]
        |> shouldEqual (Some { BeforePosition = 8; Position = 5 })
