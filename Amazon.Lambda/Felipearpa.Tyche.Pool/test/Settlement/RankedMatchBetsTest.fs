namespace Felipearpa.Tyche.Pool.Test.Settlement

open System
open Amazon.DynamoDBv2.Model
open Felipearpa.Core.Paging
open Felipearpa.Tyche.Pool.Application
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Test.Fakes
open Felipearpa.Tyche.Pool.Test.Settlement.PoolScenario
open Felipearpa.Type
open FsUnitTyped
open Xunit

/// The ranked participant feed: order, paging, and the authoritative access checks.
module RankedMatchBetsTest =

    let private pool = Ulid.newOf poolId

    /// D scored an exact 2-1 on match 0 (20 points) and has no bet on matches 1 and 2.
    let private leaderWithoutBet =
        { GamblerId = gamblerD
          Total = 20
          Position = Some 1
          Predictions = [ match0, (2, 1) ] }

    let private ready (options: HarnessOptions) (members: MemberSeed list) =
        async {
            let harness = VersionedSettlementHarness(options)
            do! harness.SeedAndMigrateAsync(members, defaultMatches)
            do! harness.RunJobsAsync [ VerifyReadiness pool ]
            return harness
        }

    let private page (harness: VersionedSettlementHarness) requester matchId cursor =
        harness.Ranked.ExecuteAsync(Ulid.newOf requester, pool, Ulid.newOf matchId, cursor)

    let private gamblers (result: Result<PoolGamblerBet CursorPage, RankedMatchBetsFailure>) =
        match result with
        | Ok page -> page.Items |> Seq.map (fun bet -> bet.GamblerId.Value) |> List.ofSeq
        | Error error -> failwith $"unexpected {error}"

    let rec private all harness requester matchId cursor acc =
        async {
            match! page harness requester matchId cursor with
            | Ok current ->
                let acc = acc @ (current.Items |> Seq.map (fun bet -> bet.GamblerId.Value) |> List.ofSeq)

                match current.Next with
                | Some _ -> return! all harness requester matchId current.Next acc
                | None -> return acc
            | Error error -> return failwith $"unexpected {error}"
        }

    [<Fact>]
    let ``the pool leader comes first even without a bet on the match`` () =
        async {
            let! harness = ready HarnessOptions.Default (defaultMembers @ [ leaderWithoutBet ])

            let! result = page harness gamblerA match1 None

            gamblers result |> shouldEqual [ gamblerD; gamblerB; gamblerC; gamblerA ]

            match result with
            | Ok page ->
                let leader = page.Items |> Seq.head
                leader.BetScore |> shouldEqual None
                leader.isComputed |> shouldEqual false
            | Error _ -> ()
        }

    [<Fact>]
    let ``ranking follows pool totals, not this match's award, on new and older matches alike`` () =
        async {
            let! harness = ready HarnessOptions.Default (defaultMembers @ [ leaderWithoutBet ])
            do! harness.SettleAsync(match1, 1, 0)

            // A: 24 (+20 here), D: 20 (0 here, no bet), B: 10 (0), C: 6 (0).
            let! match1Result = page harness gamblerA match1 None
            gamblers match1Result |> shouldEqual [ gamblerA; gamblerD; gamblerB; gamblerC ]

            match match1Result with
            | Ok page -> page.Items |> Seq.map _.Score |> List.ofSeq |> shouldEqual [ Some 20; Some 0; Some 0; Some 0 ]
            | Error _ -> ()

            let! older = page harness gamblerA match0 None
            gamblers older |> shouldEqual [ gamblerA; gamblerD; gamblerB; gamblerC ]

            match older with
            | Ok page -> page.Items |> Seq.map _.Score |> List.ofSeq |> shouldEqual [ Some 4; Some 20; Some 10; Some 6 ]
            | Error _ -> ()
        }

    [<Fact>]
    let ``a large all-zero tie pages in gambler ID order exactly once`` () =
        async {
            let zeroMembers =
                [ for index in 1..9 ->
                      { GamblerId = $"01K1PX1TX2NM1HG851S1V0Z{index:D3}"
                        Total = 0
                        Position = None
                        Predictions = [ match0, (0, 5) ] } ]

            let! harness = ready { HarnessOptions.Default with RankedPageSize = 2 } zeroMembers
            let requester = zeroMembers.Head.GamblerId

            let! traversed = all harness requester match1 None []

            traversed |> shouldEqual (zeroMembers |> List.map _.GamblerId |> List.sort)
        }

    [<Fact>]
    let ``unavailable totals come last after confirmed zero`` () =
        async {
            let zeroMember =
                { GamblerId = "01K1PX1TX2NM1HG851S1V0Z001"
                  Total = 0
                  Position = None
                  Predictions = [ match0, (0, 5) ] }

            let! harness = ready HarnessOptions.Default (defaultMembers @ [ zeroMember ])

            // B's copy is unavailable, as a malformed source total would project.
            let row = detail harness.Fake gamblerB match1 |> Option.get
            row.Remove "totalPoolPoints" |> ignore
            row["totalPoolPointsState"] <- AttributeValue(S = "UNAVAILABLE")
            row["getPoolMatchBetsByTotalSk"] <- AttributeValue(S = RankingKey.sortKey UnavailableTotal (Ulid.newOf gamblerB))
            harness.Fake.Put("Pool", row)

            let! result = page harness gamblerA match1 None

            gamblers result |> shouldEqual [ gamblerC; gamblerA; zeroMember.GamblerId; gamblerB ]
        }

    [<Fact>]
    let ``a page is read from the ranking index alone, without per-row enrichment`` () =
        async {
            let! harness = ready HarnessOptions.Default defaultMembers
            let queries = harness.Fake.CallCount "Query"
            let gets = harness.Fake.CallCount "GetItem"
            let batches = harness.Fake.CallCount "BatchGetItem"

            let! _ = page harness gamblerA match1 None

            harness.Fake.CallCount "Query" - queries |> shouldEqual 1
            harness.Fake.CallCount "BatchGetItem" - batches |> shouldEqual 0
            // Pool root, requester membership, and layout match: the access checks only.
            harness.Fake.CallCount "GetItem" - gets |> shouldEqual 3
        }

    [<Fact>]
    let ``cursors from another match are rejected and malformed cursors fail recoverably`` () =
        async {
            let! harness = ready { HarnessOptions.Default with RankedPageSize = 1 } defaultMembers
            let! first = page harness gamblerA match1 None

            let cursor =
                match first with
                | Ok current -> current.Next
                | Error error -> failwith $"unexpected {error}"

            let! crossMatch = page harness gamblerA match0 cursor
            let! malformed = page harness gamblerA match1 (Some "garbage")

            crossMatch |> shouldEqual (Error(InvalidCursor CursorContextMismatch))
            malformed |> shouldEqual (Error(InvalidCursor MalformedCursor))
        }

    [<Fact>]
    let ``non-members and removed requesters are denied while their rows remain`` () =
        async {
            let! harness = ready HarnessOptions.Default defaultMembers
            let! outsider = page harness gamblerD match1 None
            outsider |> shouldEqual (Error NotPoolMember)

            // Removal committed; the stream cleanup has not run yet.
            do! harness.RemoveAsync gamblerC
            let! removed = page harness gamblerC match1 None

            removed |> shouldEqual (Error NotPoolMember)
            detail harness.Fake gamblerC match1 |> Option.isSome |> shouldEqual true
        }

    let private reschedule (harness: VersionedSettlementHarness) matchId (kickoff: DateTime) =
        let key =
            dict
                [ "pk", AttributeValue(S = $"POOLLAYOUT#{poolLayoutId}")
                  "sk", AttributeValue(S = $"MATCH#{matchId}") ]

        let item = harness.Fake.TryGet("PoolLayout", key) |> Option.get
        item["matchDateTime"] <- AttributeValue(S = kickoff.ToUniversalTime().ToString("o"))
        harness.Fake.Put("PoolLayout", item)

    [<Fact>]
    let ``predictions stay hidden before the lock, including after a reschedule reopens them`` () =
        async {
            let! harness = ready HarnessOptions.Default defaultMembers

            reschedule harness match2 (DateTime.UtcNow.AddHours 2.0)
            let! reopened = page harness gamblerA match2 None
            reopened |> shouldEqual (Error PredictionsHidden)

            reschedule harness match2 (DateTime.UtcNow.AddMinutes 9.0)
            let! insideLock = page harness gamblerA match2 None
            insideLock |> Result.isOk |> shouldEqual true
        }

    [<Fact>]
    let ``a computed match is revealed even if its kickoff moved to the future`` () =
        async {
            let! harness = ready HarnessOptions.Default defaultMembers
            do! harness.SettleAsync(match1, 1, 0)
            reschedule harness match1 (DateTime.UtcNow.AddDays 1.0)

            let! result = page harness gamblerA match1 None

            result |> Result.isOk |> shouldEqual true
        }

    [<Fact>]
    let ``an unready pool fails recoverably instead of using another order`` () =
        async {
            let harness = VersionedSettlementHarness()
            do! harness.SeedAndMigrateAsync(defaultMembers, defaultMatches)
            let! unready = page harness gamblerA match1 None
            unready |> shouldEqual (Error RankingNotReady)
        }

    [<Fact>]
    let ``unknown pools and matches are reported, not guessed`` () =
        async {
            let! harness = ready HarnessOptions.Default defaultMembers
            let! unknownMatch = page harness gamblerA "01K1PX1TX2NM1HG851S1V0M099" None
            let! unknownPool = harness.Ranked.ExecuteAsync(Ulid.newOf gamblerA, Ulid.newOf "01K1PX1TX2NM1HG851S1V0P099", Ulid.newOf match1, None)

            unknownMatch |> shouldEqual (Error MatchNotFound)
            unknownPool |> shouldEqual (Error RankedMatchBetsFailure.PoolNotFound)
        }

    [<Fact>]
    let ``after propagation converges a refresh reflects current totals`` () =
        async {
            let! harness = ready HarnessOptions.Default defaultMembers
            let! beforeSettlement = page harness gamblerA match0 None
            gamblers beforeSettlement |> shouldEqual [ gamblerB; gamblerC; gamblerA ]

            do! harness.SettleAsync(match1, 1, 0)

            let! refreshed = page harness gamblerA match0 None
            gamblers refreshed |> shouldEqual [ gamblerA; gamblerB; gamblerC ]
        }
