namespace Felipearpa.Tyche.Pool.Test.Settlement

open System
open System.Collections.Generic
open System.Globalization
open Amazon.DynamoDBv2.Model
open Felipearpa.Tyche.Pool.Application
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Test.Settlement.PoolScenario
open Felipearpa.Type
open FsUnitTyped
open Xunit

/// A rescheduled match's kickoff follows the layout onto every detail, and the kickoff-group
/// backfill prepares settled pools for the grouped feeds; both converge without further writes.
module KickoffChangeAndBackfillTest =

    let private pool = Ulid.newOf poolId
    let private gamblerIds = [ gamblerA; gamblerB; gamblerC ]
    let private groupKickoff = pastKickoff
    let private newerKickoff = pastKickoff.AddHours 1.0
    let private iso (value: DateTime) = value.ToUniversalTime().ToString("o", CultureInfo.InvariantCulture)

    /// match1 and match2 share a kickoff; match3 kicks off an hour later.
    let private matches =
        [ { MatchId = match1
            MatchDateTime = groupKickoff
            Result = None }
          { MatchId = match2
            MatchDateTime = groupKickoff
            Result = None }
          { MatchId = match3
            MatchDateTime = newerKickoff
            Result = None } ]

    let private seeded () =
        async {
            let harness = VersionedSettlementHarness()
            do! harness.SeedAndMigrateAsync(defaultMembers, matches)
            return harness
        }

    let private moveLayoutKickoff (harness: VersionedSettlementHarness) (matchId: string) (kickoff: DateTime) =
        let key =
            dict [ "pk", AttributeValue(S = $"POOLLAYOUT#{poolLayoutId}"); "sk", AttributeValue(S = $"MATCH#{matchId}") ]

        let item = harness.Fake.TryGet("PoolLayout", key) |> Option.get
        item["matchDateTime"] <- AttributeValue(S = iso kickoff)
        harness.Fake.Put("PoolLayout", item)

    let private feeds (harness: VersionedSettlementHarness) =
        let reader = KickoffGroupReader(harness.LayoutReader)
        GetFinishedPoolGamblerBets(harness.Bets, reader), GetGamblerBetsTimeline(harness.Bets, reader)

    let private describe (item: KickoffGroupItem) =
        item.Group.KickoffDateTime,
        item.Bets |> List.map _.MatchId.Value,
        (item.Group.ComputedMatches, item.Group.TotalMatches)

    let rec private all (fetch: string option -> Async<KickoffGroupItem Felipearpa.Core.Paging.CursorPage>) next acc =
        async {
            let! page = fetch next
            let acc = acc @ List.ofSeq page.Items

            match page.Next with
            | Some _ -> return! all fetch page.Next acc
            | None -> return acc |> List.map describe
        }

    let private historyOf harness gamblerId =
        let history, _ = feeds harness
        all (fun next -> history.ExecuteAsync(pool, Ulid.newOf gamblerId, next)) None []

    let private timelineOf harness gamblerId =
        let _, timeline = feeds harness
        all (fun next -> timeline.ExecuteAsync(pool, Ulid.newOf gamblerId, next)) None []

    let private utc (value: DateTime) = KickoffGroup.instant value

    [<Fact>]
    let ``given a postponed pending match when the change propagates then both feeds regroup it`` () =
        async {
            let! harness = seeded ()
            do! harness.SettleAsync(match1, 1, 0)

            let! timelineBefore = timelineOf harness gamblerB
            timelineBefore |> List.head |> shouldEqual (utc groupKickoff, [ match1; match2 ], (1, 2))

            // match2 moves to a later kickoff whose predictions are already revealed.
            let postponed = pastKickoff.AddMinutes 90.0
            moveLayoutKickoff harness match2 postponed
            let propagate = PropagateKickoffChange(harness.DetailKickoffs)
            let! written = propagate.ExecuteAsync(Ulid.newOf match2, groupKickoff, postponed)
            written |> shouldEqual 3

            for gamblerId in gamblerIds do
                detail harness.Fake gamblerId match2 |> text "matchDateTime" |> shouldEqual (Some(iso postponed))
                detail harness.Fake gamblerId match2 |> has "computedMatchDateTime" |> shouldEqual false

            // The 17:00 group now has one match, settled; match2 forms its own pending group.
            let! timeline = timelineOf harness gamblerB

            timeline
            |> shouldEqual
                [ utc newerKickoff, [ match3 ], (0, 1)
                  utc postponed, [ match2 ], (0, 1)
                  utc groupKickoff, [ match1 ], (1, 1) ]

            let! history = historyOf harness gamblerB
            history |> List.head |> shouldEqual (utc groupKickoff, [ match1 ], (1, 1))

            // The pool's trend kickoff is not rewritten.
            root harness.Fake |> text "trendKickoffDateTime" |> shouldEqual (Some(iso groupKickoff))
        }

    [<Fact>]
    let ``given a change delivered twice when it propagates again then nothing is written`` () =
        async {
            let! harness = seeded ()
            let postponed = pastKickoff.AddDays 1.0
            moveLayoutKickoff harness match2 postponed
            let propagate = PropagateKickoffChange(harness.DetailKickoffs)

            let! first = propagate.ExecuteAsync(Ulid.newOf match2, groupKickoff, postponed)
            harness.Fake.TakeChanges() |> ignore
            let updates = harness.Fake.CallCount "UpdateItem"
            let! second = propagate.ExecuteAsync(Ulid.newOf match2, groupKickoff, postponed)

            (first, second) |> shouldEqual (3, 0)
            harness.Fake.TakeChanges() |> shouldEqual []
            // Not even a conditional write is attempted.
            harness.Fake.CallCount "UpdateItem" |> shouldEqual updates
        }

    [<Fact>]
    let ``given a settled match is rescheduled when the change propagates then both of its copies move`` () =
        async {
            let! harness = seeded ()
            do! harness.SettleAsync(match1, 1, 0)
            let moved = pastKickoff.AddMinutes 45.0
            moveLayoutKickoff harness match1 moved

            let! written = PropagateKickoffChange(harness.DetailKickoffs).ExecuteAsync(Ulid.newOf match1, groupKickoff, moved)
            written |> shouldEqual 3

            for gamblerId in gamblerIds do
                detail harness.Fake gamblerId match1 |> text "matchDateTime" |> shouldEqual (Some(iso moved))
                detail harness.Fake gamblerId match1 |> text "computedMatchDateTime" |> shouldEqual (Some(iso moved))

            let! history = historyOf harness gamblerA
            history |> List.head |> shouldEqual (utc moved, [ match1 ], (1, 1))
        }

    [<Fact>]
    let ``given a stale index entry when the change propagates then the detail is re-read and not overwritten`` () =
        async {
            let! harness = seeded ()
            let postponed = pastKickoff.AddDays 1.0
            harness.Fake.FreezeIndexes()

            // A detail moved elsewhere after the index was frozen keeps its own value.
            let elsewhere = pastKickoff.AddDays 3.0
            let item = detail harness.Fake gamblerC match2 |> Option.get
            item["matchDateTime"] <- AttributeValue(S = iso elsewhere)
            harness.Fake.Put("Pool", item)

            moveLayoutKickoff harness match2 postponed
            let! written = PropagateKickoffChange(harness.DetailKickoffs).ExecuteAsync(Ulid.newOf match2, groupKickoff, postponed)
            harness.Fake.ReleaseIndexes()

            written |> shouldEqual 2
            detail harness.Fake gamblerC match2 |> text "matchDateTime" |> shouldEqual (Some(iso elsewhere))
            detail harness.Fake gamblerA match2 |> text "matchDateTime" |> shouldEqual (Some(iso postponed))
        }

    /// Removes what the kickoff-group change added, as a pool settled before it looks.
    let private asBeforeTheChange (harness: VersionedSettlementHarness) =
        for gamblerId in gamblerIds do
            for matchId in [ match0; match1; match2; match3 ] do
                detail harness.Fake gamblerId matchId
                |> Option.iter (fun item ->
                    item.Remove "computedMatchDateTime" |> ignore
                    harness.Fake.Put("Pool", item))

        let rootItem = root harness.Fake |> Option.get
        rootItem.Remove "trendKickoffDateTime" |> ignore
        harness.Fake.Put("Pool", rootItem)
        harness.Fake.TakeChanges() |> ignore

    [<Fact>]
    let ``given a pool settled mid-tournament when the backfill runs then computed details get their kickoffs and the latest receipt sets the trend kickoff`` () =
        async {
            let! harness = seeded ()
            do! harness.SettleAsync(match3, 0, 0)
            do! harness.SettleAsync(match1, 1, 0)
            asBeforeTheChange harness
            let baselines = gamblerIds |> List.map (beforePosition harness.Fake)

            do! harness.RunJobsAsync [ BackfillKickoffGroups(pool, None) ]

            for gamblerId in gamblerIds do
                for matchId in [ match0; match1; match3 ] do
                    let copied = detail harness.Fake gamblerId matchId
                    copied |> text "computedMatchDateTime" |> shouldEqual (copied |> text "matchDateTime")

                detail harness.Fake gamblerId match2 |> has "computedMatchDateTime" |> shouldEqual false

            // The most recently completed result settlement was match1's, even though match3 is newer.
            root harness.Fake |> text "trendKickoffDateTime" |> shouldEqual (Some(iso groupKickoff))
            gamblerIds |> List.map (beforePosition harness.Fake) |> shouldEqual baselines

            let! history = historyOf harness gamblerA
            history |> List.map (fun (kickoff, _, _) -> kickoff) |> shouldEqual [ utc newerKickoff; utc groupKickoff; utc historicalMatch.MatchDateTime ]

            // A rerun completes without writes.
            harness.Fake.TakeChanges() |> ignore
            do! harness.RunJobsAsync [ BackfillKickoffGroups(pool, None) ]
            harness.Fake.TakeChanges() |> shouldEqual []
        }

    /// Deletes the pool's result receipts, as a pool settled before receipts existed looks.
    let private withoutReceipts (harness: VersionedSettlementHarness) =
        async {
            let receipts =
                harness.Fake.Items "Pool"
                |> List.filter (fun item -> item["pk"].S = $"POOL#{poolId}" && item["sk"].S.StartsWith "SETTLEMENT#RESULT#")

            receipts |> List.isEmpty |> shouldEqual false

            for receipt in receipts do
                let key = dict [ "pk", receipt["pk"]; "sk", receipt["sk"] ]

                do!
                    harness.Fake.Client.DeleteItemAsync(DeleteItemRequest(TableName = "Pool", Key = Dictionary key))
                    |> Async.AwaitTask
                    |> Async.Ignore

            harness.Fake.TakeChanges() |> ignore
        }

    [<Fact>]
    let ``given a pool settled before receipts existed when the backfill runs then the latest computed kickoff becomes its trend kickoff`` () =
        async {
            let! harness = seeded ()
            do! harness.SettleAsync(match3, 0, 0)
            do! harness.SettleAsync(match1, 1, 0)
            asBeforeTheChange harness
            do! withoutReceipts harness
            let baselines = gamblerIds |> List.map (beforePosition harness.Fake)

            do! harness.RunJobsAsync [ BackfillKickoffGroups(pool, None) ]

            // match3 kicks off latest among the computed matches; match1 settled last but has no receipt.
            root harness.Fake |> text "trendKickoffDateTime" |> shouldEqual (Some(iso newerKickoff))
            gamblerIds |> List.map (beforePosition harness.Fake) |> shouldEqual baselines

            for gamblerId in gamblerIds do
                let copied = detail harness.Fake gamblerId match3
                copied |> text "computedMatchDateTime" |> shouldEqual (copied |> text "matchDateTime")

            // A rerun completes without writes.
            harness.Fake.TakeChanges() |> ignore
            do! harness.RunJobsAsync [ BackfillKickoffGroups(pool, None) ]
            harness.Fake.TakeChanges() |> shouldEqual []
        }

    [<Fact>]
    let ``given a pool whose only computed match predates receipts when the backfill runs then that kickoff becomes its trend kickoff`` () =
        async {
            let! harness = seeded ()
            asBeforeTheChange harness

            do! harness.RunJobsAsync [ BackfillKickoffGroups(pool, None) ]

            root harness.Fake |> text "trendKickoffDateTime" |> shouldEqual (Some(iso historicalMatch.MatchDateTime))
            detail harness.Fake gamblerA match0 |> text "computedMatchDateTime" |> shouldEqual (detail harness.Fake gamblerA match0 |> text "matchDateTime")
        }

    [<Fact>]
    let ``given a pool without computed details when the backfill runs then it records no trend kickoff`` () =
        async {
            let! harness = seeded ()
            asBeforeTheChange harness

            for gamblerId in gamblerIds do
                let item = detail harness.Fake gamblerId match0 |> Option.get
                item.Remove "computedRequestId" |> ignore
                harness.Fake.Put("Pool", item)

            harness.Fake.TakeChanges() |> ignore

            do! harness.RunJobsAsync [ BackfillKickoffGroups(pool, None) ]

            root harness.Fake |> has "trendKickoffDateTime" |> shouldEqual false
            harness.Fake.TakeChanges() |> shouldEqual []
        }

    [<Fact>]
    let ``given an active operation when the backfill records the trend kickoff then it waits for the pool to be idle`` () =
        async {
            let! harness = seeded ()
            do! harness.SettleAsync(match1, 1, 0)
            asBeforeTheChange harness

            // Admit a settlement and stop it at once, so it stays active on the root.
            harness.SetLayoutResult(match3, 0, 0)

            let command =
                PoolCommand.settleResult
                    pool
                    (Ulid.newOf match3)
                    { HomeTeamScore = 0
                      AwayTeamScore = 0
                      Version = 1 }

            let! yielded = harness.ProcessCommand.ExecuteAsync(command, harness.Now())
            yielded.Outcome |> shouldEqual PoolCommandOutcome.Yielded

            let backfill =
                BackfillPoolKickoffGroups(harness.Settlement, harness.DetailKickoffs, harness.LayoutReader, harness.MaintenancePublisher)

            let! failed = backfill.ExecuteAsync(pool, None) |> Async.Catch

            match failed with
            | Choice1Of2 report -> failwith $"expected the job to be retried, got %A{report}"
            | Choice2Of2 error -> (error :? InvalidOperationException) |> shouldEqual true

            root harness.Fake |> has "trendKickoffDateTime" |> shouldEqual false

            // The operation completes and records its own kickoff; the retried job leaves it.
            let! resumed = harness.ProcessCommand.ExecuteAsync(command, harness.Now().AddMinutes 10.0)
            resumed.Outcome |> shouldEqual PoolCommandOutcome.OperationCompleted
            let! report = backfill.ExecuteAsync(pool, None)

            report.RecordedTrendKickoff |> shouldEqual None
            root harness.Fake |> text "trendKickoffDateTime" |> shouldEqual (Some(iso newerKickoff))
        }

    [<Fact>]
    let ``a trend kickoff write is refused once an operation was admitted after the root was read`` () =
        async {
            let! harness = seeded ()
            do! harness.SettleAsync(match1, 1, 0)
            asBeforeTheChange harness
            let! observed = harness.Settlement.GetRootAsync pool

            do! harness.SettleAsync(match3, 0, 0)
            let rootItem = root harness.Fake |> Option.get
            rootItem.Remove "trendKickoffDateTime" |> ignore
            harness.Fake.Put("Pool", rootItem)

            let! outcome = harness.Settlement.RecordTrendKickoffAsync(observed.Value, groupKickoff)

            outcome |> shouldEqual ConditionNotMet
            root harness.Fake |> has "trendKickoffDateTime" |> shouldEqual false
        }
