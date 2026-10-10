namespace Felipearpa.Tyche.Pool.Test.Settlement

open System
open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Core.Paging
open Felipearpa.Tyche.Pool.Application
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Infrastructure
open Felipearpa.Tyche.Pool.Test.Fakes
open Felipearpa.Tyche.Pool.Test.Settlement.PoolScenario
open Felipearpa.Tyche.Pool.Type
open Felipearpa.Type
open FsUnitTyped
open Xunit

/// Restoration of a rejoining gambler's retained history: the guarded rebinding of one
/// detail, the ordered restore operation, how it is requested, and the end-to-end outcome.
///
/// In every scenario D joins, predicts 3-1 on match 1 and 0-2 on match 2, earns 10 when
/// match 1 ends 1-0 and 4 when match 2 ends 0-0, and is removed. A then has 44, B 14, C 6.
/// D's join also gave D a zero-point late-join detail for the historical match 0, so three
/// computed details are retained.
module RejoinRestorationTest =

    let private pool = Ulid.newOf poolId
    let private gambler = Ulid.newOf gamblerD

    /// Matches still open when the scenario starts, so predictions go through the bet path.
    let private openMatches =
        [ { MatchId = match1
            MatchDateTime = DateTime.UtcNow.AddDays 1.0
            Result = None }
          { MatchId = match2
            MatchDateTime = DateTime.UtcNow.AddDays 2.0
            Result = None } ]

    let private predict (harness: VersionedSettlementHarness) (gamblerId: string) (matchId: string) (home, away) =
        async {
            let score =
                { TeamScore.HomeTeamValue = BetScore.newOf home
                  AwayTeamValue = BetScore.newOf away }

            match! harness.Bets.BetAsync(pool, Ulid.newOf gamblerId, Ulid.newOf matchId, score) with
            | Ok _ -> ()
            | Error failure -> failwith $"prediction rejected: {failure}"
        }

    let private generation (harness: VersionedSettlementHarness) =
        membership harness.Fake gamblerD |> text "membershipGeneration"

    let private earned (options: HarnessOptions) =
        async {
            let harness = VersionedSettlementHarness(options)
            do! harness.SeedAndMigrateAsync(defaultMembers, openMatches)
            let! _ = harness.JoinAsync gamblerD
            do! harness.RunUntilQuiescentAsync()
            do! predict harness gamblerD match1 (3, 1)
            do! predict harness gamblerD match2 (0, 2)
            do! harness.SettleAsync(match1, 1, 0)
            do! harness.SettleAsync(match2, 0, 0)
            return harness
        }

    /// D earned 14 points and was removed; the two computed details are retained.
    let private removed (options: HarnessOptions) =
        async {
            let! harness = earned options
            do! harness.RemoveAsync gamblerD
            do! harness.RunUntilQuiescentAsync()
            return harness
        }

    /// D rejoined, but no stream record was delivered, so nothing requested the restore and
    /// the join materialized nothing.
    let private rejoinedUnrestored (options: HarnessOptions) =
        async {
            let! harness = removed options
            let! _ = harness.JoinAsync gamblerD
            harness.Fake.TakeChanges() |> ignore
            return harness
        }

    let private restoreCommand (harness: VersionedSettlementHarness) =
        PoolCommand.restoreRejoinHistory pool gambler (generation harness |> Option.get)

    let private restore (harness: VersionedSettlementHarness) =
        harness.ProcessCommand.ExecuteAsync(restoreCommand harness, DateTime.UtcNow.AddMinutes 5.0)

    let private receipt (harness: VersionedSettlementHarness) operationId =
        harness.Fake.TryGet(
            "Pool",
            dict [ "pk", AttributeValue(S = $"POOL#{poolId}"); "sk", AttributeValue(S = $"SETTLEMENT#{operationId}") ]
        )

    let private isRebinding (request: obj) =
        match request with
        | :? TransactWriteItemsRequest as transaction ->
            transaction.TransactItems
            |> Seq.exists (fun item ->
                not (isNull item.Update) && item.Update.UpdateExpression.Contains "#restoreExcludedGeneration")
        | _ -> false

    let private rebindsMatch (matchId: string) (request: obj) =
        isRebinding request
        && (request :?> TransactWriteItemsRequest).TransactItems
           |> Seq.exists (fun item -> not (isNull item.Update) && item.Update.Key["sk"].S = $"MATCH#{matchId}")

    let private conditionFailure () =
        TransactionCanceledException(
            "Transaction cancelled [ConditionalCheckFailed, None]",
            CancellationReasons =
                List([ CancellationReason(Code = "ConditionalCheckFailed"); CancellationReason(Code = "None") ])
        )

    /// The attributes a restoration must never change on a detail.
    let private preserved (item: Dictionary<string, AttributeValue> option) =
        item
        |> Option.map (fun found ->
            [ "homeTeamBet"; "awayTeamBet"; "position"; "beforePosition"; "positionOperationRevision"; "computedDateTime" ]
            |> List.map (fun attribute ->
                attribute,
                match found.TryGetValue attribute with
                | true, value -> Some(if isNull value.S then value.N else value.S)
                | _ -> None))

    let private snapshot (item: Dictionary<string, AttributeValue> option) =
        item
        |> Option.map (fun found ->
            found
            |> Seq.map (fun pair ->
                pair.Key,
                (if not (isNull pair.Value.S) then pair.Value.S
                 elif not (isNull pair.Value.N) then pair.Value.N
                 else string pair.Value.BOOL))
            |> List.ofSeq
            |> List.sort)

    let private bool (attribute: string) (item: Dictionary<string, AttributeValue> option) =
        item
        |> Option.bind (fun found ->
            match found.TryGetValue attribute with
            | true, value when value.BOOL.HasValue -> Some value.BOOL.Value
            | _ -> None)

    let private assertBoundToCurrentTotal (harness: VersionedSettlementHarness) (matchId: string) =
        let current = membership harness.Fake gamblerD
        let restored = detail harness.Fake gamblerD matchId
        restored |> text "poolMembershipGeneration" |> shouldEqual (generation harness)
        restored |> bool "rankingActive" |> shouldEqual (Some true)
        restored |> number "totalPoolPoints" |> shouldEqual (current |> number "score")
        restored |> number "poolScoreVersion" |> shouldEqual (current |> number "scoreVersion")
        restored |> text "getPoolMatchBetsByTotalPk" |> shouldEqual (Some $"POOL#{poolId}#MATCH#{matchId}")

        restored |> text "getPoolMatchBetsByTotalSk"
        |> shouldEqual (current |> number "score" |> Option.map (fun total -> RankingKey.sortKey (KnownTotal total) gambler))

    // 1.2 The guarded rebinding of one retained detail.

    let private decideFor (harness: VersionedSettlementHarness) (matchId: string) (result: MatchResult) =
        async {
            let! current = harness.Settlement.GetMembershipAsync(pool, gambler)
            let! bet = harness.Settlement.GetRetainedBetAsync(pool, gambler, Ulid.newOf matchId)
            return RestorePolicy.decide (generation harness |> Option.get) current bet.Value (MatchScored result)
        }

    let private rebindingOf (harness: VersionedSettlementHarness) (matchId: string) (result: MatchResult) =
        async {
            match! decideFor harness matchId result with
            | Rebind change ->
                return
                    { RebindWrite.PoolId = pool
                      GamblerId = gambler
                      MatchId = Ulid.newOf matchId
                      OperationId = "RESTORE#TEST"
                      OperationRevision = 9L
                      Change = change }
            | other -> return failwith $"expected a rebinding, got {other}"
        }

    let private match1Result = { HomeTeamScore = 1; AwayTeamScore = 0; Version = 1 }

    [<Fact>]
    let ``a first rebinding moves the detail with its full projection and adds the award once`` () =
        async {
            let! harness = rejoinedUnrestored HarnessOptions.Default
            let before = detail harness.Fake gamblerD match1
            let! write = rebindingOf harness match1 match1Result

            let! outcome = harness.Settlement.ApplyRebindingAsync write

            outcome |> shouldEqual Written
            total harness.Fake gamblerD |> shouldEqual (Some 10)
            membership harness.Fake gamblerD |> number "scoreVersion" |> shouldEqual (Some 2)
            assertBoundToCurrentTotal harness match1
            detail harness.Fake gamblerD match1 |> number "score" |> shouldEqual (Some 10)
            detail harness.Fake gamblerD match1 |> text "settlementOperationId" |> shouldEqual (Some "RESTORE#TEST")
            detail harness.Fake gamblerD match1 |> preserved |> shouldEqual (preserved before)

            // The same write again finds the detail moved and adds nothing.
            let! repeated = harness.Settlement.ApplyRebindingAsync write
            repeated |> shouldEqual ConditionNotMet
            total harness.Fake gamblerD |> shouldEqual (Some 10)
        }

    [<Fact>]
    let ``a rebinding whose response was lost is confirmed as applied, not counted again`` () =
        async {
            let! harness = rejoinedUnrestored HarnessOptions.Default
            let lost = ref 0

            harness.Fake.Intercept(fun request ->
                if lost.Value = 0 && isRebinding request then
                    lost.Value <- 1
                    FailAfterCommit(conditionFailure ())
                else
                    Proceed)

            let! report = restore harness

            lost.Value |> shouldEqual 1
            report.Outcome |> shouldEqual PoolCommandOutcome.OperationCompleted
            report.Settled |> shouldEqual 3
            total harness.Fake gamblerD |> shouldEqual (Some 14)
        }

    [<Fact>]
    let ``a detail with no generation and no copied total is restored`` () =
        async {
            let! harness = removed HarnessOptions.Default
            let legacy = detail harness.Fake gamblerD match1 |> Option.get

            for attribute in [ "poolMembershipGeneration"; "poolScoreVersion"; "totalPoolPoints"; "totalPoolPointsState"; "rankingActive"; "resultVersion" ] do
                legacy.Remove attribute |> ignore

            harness.Fake.Put("Pool", legacy)
            let! _ = harness.JoinAsync gamblerD
            harness.Fake.TakeChanges() |> ignore

            let! report = restore harness

            report.Outcome |> shouldEqual PoolCommandOutcome.OperationCompleted
            total harness.Fake gamblerD |> shouldEqual (Some 14)
            detail harness.Fake gamblerD match1 |> number "resultVersion" |> shouldEqual (Some 1)
            detail harness.Fake gamblerD match1 |> text "poolMembershipGeneration" |> shouldEqual (generation harness)
            detail harness.Fake gamblerD match1 |> has "getPoolMatchBetsByTotalSk" |> shouldEqual true
        }

    [<Fact>]
    let ``a tombstone whose copied version exceeds the new membership's gets the new binding`` () =
        async {
            let! harness = removed HarnessOptions.Default

            for matchId in [ match1; match2 ] do
                let tombstone = detail harness.Fake gamblerD matchId |> Option.get
                tombstone["poolScoreVersion"] <- AttributeValue(N = "99")
                harness.Fake.Put("Pool", tombstone)

            let! _ = harness.JoinAsync gamblerD
            harness.Fake.TakeChanges() |> ignore
            let! write = rebindingOf harness match1 match1Result

            let! outcome = harness.Settlement.ApplyRebindingAsync write

            outcome |> shouldEqual Written
            detail harness.Fake gamblerD match1 |> number "poolScoreVersion" |> shouldEqual (Some 2)
            assertBoundToCurrentTotal harness match1
        }

    [<Fact>]
    let ``a detail whose generation changed after the read is not rebound by that read`` () =
        async {
            let! harness = rejoinedUnrestored HarnessOptions.Default
            let! write = rebindingOf harness match1 match1Result
            let moved = detail harness.Fake gamblerD match1 |> Option.get
            moved["poolMembershipGeneration"] <- AttributeValue(S = "01K1PX1TX2NM1HG851S1VOTHER")
            harness.Fake.Put("Pool", moved)

            let! outcome = harness.Settlement.ApplyRebindingAsync write

            outcome |> shouldEqual ConditionNotMet
            total harness.Fake gamblerD |> shouldEqual (Some 0)
            detail harness.Fake gamblerD match1 |> text "poolMembershipGeneration" |> shouldEqual (Some "01K1PX1TX2NM1HG851S1VOTHER")

            // The operation reads the detail again and restores it from what it finds.
            let! _ = restore harness
            total harness.Fake gamblerD |> shouldEqual (Some 14)
        }

    [<Fact>]
    let ``a result corrected during the absence restores the corrected award`` () =
        async {
            let! harness = removed HarnessOptions.Default
            // 1-0 → 3-0: the 3-1 prediction now earns 10 + 4 (home score).
            let! accepted = harness.CorrectResultAsync(match1, 3, 0)
            do! harness.RunUntilQuiescentAsync()
            accepted |> shouldEqual true
            detail harness.Fake gamblerD match1 |> number "resultVersion" |> shouldEqual (Some 1)
            let before = detail harness.Fake gamblerD match1

            let! _ = harness.JoinAsync gamblerD
            do! harness.RunUntilQuiescentAsync()

            total harness.Fake gamblerD |> shouldEqual (Some 18)
            award harness.Fake gamblerD match1 |> shouldEqual (Some 14)
            detail harness.Fake gamblerD match1 |> number "resultVersion" |> shouldEqual (Some 2)
            detail harness.Fake gamblerD match1 |> number "homeTeamScore" |> shouldEqual (Some 3)
            detail harness.Fake gamblerD match1 |> preserved |> shouldEqual (preserved before)
        }

    [<Fact>]
    let ``a removed membership fails the rebinding condition`` () =
        async {
            let! harness = rejoinedUnrestored HarnessOptions.Default
            let! write = rebindingOf harness match1 match1Result
            let tombstone = detail harness.Fake gamblerD match1 |> snapshot
            do! harness.RemoveAsync gamblerD

            let! outcome = harness.Settlement.ApplyRebindingAsync write

            outcome |> shouldEqual ConditionNotMet
            membership harness.Fake gamblerD |> shouldEqual None
            detail harness.Fake gamblerD match1 |> snapshot |> shouldEqual tombstone
        }

    [<Fact>]
    let ``a changed score version is read again and the award is added to the fresh total`` () =
        async {
            let! harness = rejoinedUnrestored HarnessOptions.Default
            let changed = ref false

            harness.Fake.Intercept(fun request ->
                if not changed.Value && isRebinding request then
                    changed.Value <- true
                    let current = membership harness.Fake gamblerD |> Option.get
                    current["score"] <- AttributeValue(N = "5")
                    current["scoreVersion"] <- AttributeValue(N = "2")
                    harness.Fake.Put("Pool", current)

                Proceed)

            let! report = restore harness

            report.Outcome |> shouldEqual PoolCommandOutcome.OperationCompleted
            total harness.Fake gamblerD |> shouldEqual (Some 19)
            membership harness.Fake gamblerD |> number "scoreVersion" |> shouldEqual (Some 4)
        }

    [<Fact>]
    let ``a malformed detail is held for repair, excluded from the total, and restored after repair`` () =
        async {
            let! harness = removed HarnessOptions.Default
            let oldGeneration = detail harness.Fake gamblerD match2 |> text "poolMembershipGeneration"
            let malformed = detail harness.Fake gamblerD match2 |> Option.get
            malformed.Remove "score" |> ignore
            harness.Fake.Put("Pool", malformed)

            let! _ = harness.JoinAsync gamblerD
            do! harness.RunUntilQuiescentAsync()

            let current = generation harness
            total harness.Fake gamblerD |> shouldEqual (Some 10)
            detail harness.Fake gamblerD match2 |> text "restoreExcludedGeneration" |> shouldEqual current
            detail harness.Fake gamblerD match2 |> text "restoreExclusionReason" |> shouldEqual (Some "computed detail without an award")
            detail harness.Fake gamblerD match2 |> text "poolMembershipGeneration" |> shouldEqual oldGeneration
            receipt harness (restoreCommand harness).OperationId |> number "unresolvedDetails" |> shouldEqual (Some 1)

            // An operator repairs the award, clears the marker, and replays the restore under a
            // new operation ID.
            let repaired = detail harness.Fake gamblerD match2 |> Option.get
            repaired["score"] <- AttributeValue(N = "4")
            repaired.Remove "restoreExcludedGeneration" |> ignore
            repaired.Remove "restoreExclusionReason" |> ignore
            harness.Fake.Put("Pool", repaired)

            do!
                harness.CommandPublisher.PublishAsync
                    [ { restoreCommand harness with OperationId = $"{(restoreCommand harness).OperationId}#OPERATOR-1" } ]

            do! harness.RunUntilQuiescentAsync()

            total harness.Fake gamblerD |> shouldEqual (Some 14)
            assertBoundToCurrentTotal harness match2
        }

    // 1.3 The ordered restore operation.

    let private restoreRevision (harness: VersionedSettlementHarness) =
        receipt harness (restoreCommand harness).OperationId |> number "operationRevision"

    /// Whether the restore operation wrote current positions: its position pass stamps every
    /// membership with the operation's revision.
    let private restoreWrotePositions (harness: VersionedSettlementHarness) =
        [ gamblerA; gamblerB; gamblerC; gamblerD ]
        |> List.exists (fun gamblerId ->
            membership harness.Fake gamblerId |> number "positionOperationRevision" = restoreRevision harness)

    let private yieldedRestore (harness: VersionedSettlementHarness) =
        harness.CommandOutcomes
        |> List.exists (fun report ->
            report.Outcome = PoolCommandOutcome.Yielded && report.ActiveOperationId = Some (restoreCommand harness).OperationId)

    let private rejoined (options: HarnessOptions) =
        async {
            let! harness = removed options
            let! _ = harness.JoinAsync gamblerD
            do! harness.RunUntilQuiescentAsync()
            return harness
        }

    [<Fact>]
    let ``a rejoin restores 10 + 4 = 14 points, keeps predictions and awards, and recalculates positions`` () =
        async {
            let! harness = removed HarnessOptions.Default
            let before = [ match1; match2 ] |> List.map (fun matchId -> detail harness.Fake gamblerD matchId |> preserved)
            let trends = [ gamblerA; gamblerB; gamblerC ] |> List.map (beforePosition harness.Fake)

            let! _ = harness.JoinAsync gamblerD
            do! harness.RunUntilQuiescentAsync()

            total harness.Fake gamblerD |> shouldEqual (Some 14)
            [ award harness.Fake gamblerD match1; award harness.Fake gamblerD match2 ] |> shouldEqual [ Some 10; Some 4 ]
            [ match1; match2 ] |> List.map (fun matchId -> detail harness.Fake gamblerD matchId |> preserved) |> shouldEqual before

            // A 44, B 14, D 14 (ties by gambler ID), C 6; nobody's trend baseline moves.
            [ gamblerA; gamblerB; gamblerD; gamblerC ] |> List.map (position harness.Fake) |> shouldEqual [ Some 1; Some 2; Some 3; Some 4 ]
            [ gamblerA; gamblerB; gamblerC ] |> List.map (beforePosition harness.Fake) |> shouldEqual trends
            beforePosition harness.Fake gamblerD |> shouldEqual None

            receipt harness (restoreCommand harness).OperationId |> text "operationState" |> shouldEqual (Some "COMPLETED")
            restoreWrotePositions harness |> shouldEqual true
        }

    [<Fact>]
    let ``details retained from two earlier memberships are each restored once`` () =
        async {
            let! harness = removed HarnessOptions.Default
            let older = detail harness.Fake gamblerD match2 |> Option.get
            older["poolMembershipGeneration"] <- AttributeValue(S = "01K1PX1TX2NM1HG851S1VOLDER")
            harness.Fake.Put("Pool", older)

            let! _ = harness.JoinAsync gamblerD
            do! harness.RunUntilQuiescentAsync()

            total harness.Fake gamblerD |> shouldEqual (Some 14)

            for matchId in [ match1; match2 ] do
                detail harness.Fake gamblerD matchId |> text "poolMembershipGeneration" |> shouldEqual (generation harness)
        }

    [<Fact>]
    let ``a first join has nothing to restore and completes without changes`` () =
        async {
            let harness = VersionedSettlementHarness()
            do! harness.SeedAndMigrateAsync(defaultMembers, openMatches)

            let! _ = harness.JoinAsync gamblerD
            do! harness.RunUntilQuiescentAsync()

            total harness.Fake gamblerD |> shouldEqual (Some 0)
            membership harness.Fake gamblerD |> number "scoreVersion" |> shouldEqual (Some 1)
            receipt harness (restoreCommand harness).OperationId |> text "operationState" |> shouldEqual (Some "COMPLETED")
            receipt harness (restoreCommand harness).OperationId |> number "unresolvedDetails" |> shouldEqual (Some 0)
            restoreWrotePositions harness |> shouldEqual false
            position harness.Fake gamblerD |> shouldEqual (Some 4)
        }

    let private pagedOneByOne =
        { HarnessOptions.Default with
            Settings = { PoolOperationSettings.Default with MembershipPageSize = 1 } }

    [<Fact>]
    let ``a restore that yields after rebinding still recalculates positions when it resumes`` () =
        async {
            let! harness = removed pagedOneByOne

            // A match added during the absence gives the rejoined membership a detail after the
            // retained ones, so the pass has one more page after the last rebinding.
            do!
                harness.HandleLayoutRecordsAsync
                    [ MatchInserted
                          { MatchId = match3
                            MatchDateTime = DateTime.UtcNow.AddDays 3.0
                            Result = None } ]

            do! harness.RunUntilQuiescentAsync()
            let yielded = ref false

            // The deadline passes right after the last retained detail is rebound, so the
            // operation yields with that page checkpointed; the page it resumes with rebinds
            // nothing, and only the restored count in the checkpoint says positions are due.
            harness.Fake.Intercept(fun request ->
                if not yielded.Value && rebindsMatch match2 request then
                    yielded.Value <- true
                    harness.AdvanceClock(TimeSpan.FromHours 2.0)

                Proceed)

            let! _ = harness.JoinAsync gamblerD
            do! harness.RunUntilQuiescentAsync()

            yielded.Value |> shouldEqual true
            yieldedRestore harness |> shouldEqual true
            total harness.Fake gamblerD |> shouldEqual (Some 14)
            restoreWrotePositions harness |> shouldEqual true
            position harness.Fake gamblerD |> shouldEqual (Some 3)
        }

    [<Fact>]
    let ``a restore that rebinds nothing before or after a yield recalculates no positions`` () =
        async {
            let harness = VersionedSettlementHarness(pagedOneByOne)
            do! harness.SeedAndMigrateAsync(defaultMembers, openMatches)
            let! _ = harness.JoinAsync gamblerD
            let! _ = harness.DeliverStreamAsync()
            let yielded = ref false

            // The deadline passes while the restore reads its first page of D's details.
            harness.Fake.Intercept(fun request ->
                match request with
                | :? QueryRequest as query when
                    not yielded.Value
                    && not (isNull query.ExpressionAttributeValues)
                    && query.ExpressionAttributeValues.ContainsKey ":pk"
                    && query.ExpressionAttributeValues[":pk"].S = $"GAMBLER#{gamblerD}#POOL#{poolId}"
                    ->
                    yielded.Value <- true
                    harness.AdvanceClock(TimeSpan.FromHours 2.0)
                    Proceed
                | _ -> Proceed)

            do! harness.RunUntilQuiescentAsync()

            yielded.Value |> shouldEqual true
            yieldedRestore harness |> shouldEqual true
            receipt harness (restoreCommand harness).OperationId |> text "operationState" |> shouldEqual (Some "COMPLETED")
            restoreWrotePositions harness |> shouldEqual false
        }

    [<Fact>]
    let ``a duplicate restore command after completion is a no-op`` () =
        async {
            let! harness = rejoined HarnessOptions.Default

            let! report = restore harness

            report.Outcome |> shouldEqual PoolCommandOutcome.AlreadyCompleted
            total harness.Fake gamblerD |> shouldEqual (Some 14)
        }

    [<Fact>]
    let ``an interrupted restore resumes on redelivery without counting an award twice`` () =
        async {
            let options =
                { HarnessOptions.Default with
                    Settings = { PoolOperationSettings.Default with MembershipPageSize = 1 } }

            let! harness = removed options
            let failures = ref 0

            // The second rebinding commits, then the invocation fails before it learns so.
            harness.Fake.Intercept(fun request ->
                if failures.Value = 0 && rebindsMatch match2 request then
                    failures.Value <- 1
                    FailAfterCommit(ProvisionedThroughputExceededException "Throughput exceeded")
                else
                    Proceed)

            let! _ = harness.JoinAsync gamblerD
            do! harness.RunUntilQuiescentAsync()

            failures.Value |> shouldEqual 1
            total harness.Fake gamblerD |> shouldEqual (Some 14)
            receipt harness (restoreCommand harness).OperationId |> text "operationState" |> shouldEqual (Some "COMPLETED")
        }

    [<Fact>]
    let ``a restore checkpoints its pages and yields at the deadline`` () =
        async {
            let options =
                { HarnessOptions.Default with
                    Settings = { PoolOperationSettings.Default with MembershipPageSize = 1 }
                    Tick = TimeSpan.FromSeconds 1.0
                    InvocationBudget = TimeSpan.FromSeconds 6.0 }

            let! harness = rejoined options

            harness.CommandOutcomes
            |> List.exists (fun report ->
                report.Outcome = PoolCommandOutcome.Yielded && report.ActiveOperationId = Some (restoreCommand harness).OperationId)
            |> shouldEqual true

            total harness.Fake gamblerD |> shouldEqual (Some 14)
        }

    [<Fact>]
    let ``a result settled while the restore is queued is counted together with the restored awards`` () =
        async {
            let harness = VersionedSettlementHarness()
            do! harness.SeedAndMigrateAsync(defaultMembers, openMatches)
            let! _ = harness.JoinAsync gamblerD
            do! harness.RunUntilQuiescentAsync()
            do! predict harness gamblerD match1 (3, 1)
            do! harness.SettleAsync(match1, 1, 0)
            do! harness.RemoveAsync gamblerD
            do! harness.RunUntilQuiescentAsync()

            // The rejoin queues standings and the restore; match 2 is predicted again and its
            // result settles before the queued restore runs.
            let! _ = harness.JoinAsync gamblerD
            let! _ = harness.DeliverStreamAsync()
            do! predict harness gamblerD match2 (0, 2)
            harness.SetLayoutResult(match2, 0, 0)

            let! settled =
                harness.ProcessCommand.ExecuteAsync(
                    PoolCommand.settleResult pool (Ulid.newOf match2) { HomeTeamScore = 0; AwayTeamScore = 0; Version = 1 },
                    DateTime.UtcNow.AddMinutes 5.0
                )

            settled.Outcome |> shouldEqual PoolCommandOutcome.OperationCompleted
            total harness.Fake gamblerD |> shouldEqual (Some 4)

            do! harness.RunUntilQuiescentAsync()

            total harness.Fake gamblerD |> shouldEqual (Some 14)
            [ award harness.Fake gamblerD match1; award harness.Fake gamblerD match2 ] |> shouldEqual [ Some 10; Some 4 ]
            assertBoundToCurrentTotal harness match1
            assertBoundToCurrentTotal harness match2
        }

    [<Fact>]
    let ``a removal during restoration stops it, and a later rejoin restores everything`` () =
        async {
            let options =
                { HarnessOptions.Default with
                    Settings = { PoolOperationSettings.Default with MembershipPageSize = 1 } }

            let! harness = removed options
            let removedMidway = ref false

            harness.Fake.Intercept(fun request ->
                if not removedMidway.Value && rebindsMatch match2 request then
                    removedMidway.Value <- true
                    harness.RemoveAsync gamblerD |> Async.RunSynchronously

                Proceed)

            let! _ = harness.JoinAsync gamblerD
            do! harness.RunUntilQuiescentAsync()
            harness.Fake.ClearInterception()

            removedMidway.Value |> shouldEqual true
            membership harness.Fake gamblerD |> shouldEqual None
            [ match1; match2 ] |> List.map (isComputed harness.Fake gamblerD) |> shouldEqual [ true; true ]

            // Both details stay retained, one under the membership removed midway.
            let! _ = harness.JoinAsync gamblerD
            do! harness.RunUntilQuiescentAsync()

            total harness.Fake gamblerD |> shouldEqual (Some 14)
            assertBoundToCurrentTotal harness match1
            assertBoundToCurrentTotal harness match2
        }

    // 1.4 Requesting the restore.

    let private recordingPublisher () =
        let published = ref []

        { new IPoolCommandPublisher with
            member _.PublishAsync(commands) = async { published.Value <- published.Value @ commands }
            member _.RepublishAsync(commands) = async { published.Value <- published.Value @ commands } },
        published

    let private reconciliation (harness: VersionedSettlementHarness) publisher =
        Felipearpa.Tyche.Pool.Application.ReconcilePool(
            harness.Settlement,
            harness.Projection,
            harness.Bets,
            harness.LayoutReader,
            publisher,
            harness.MaintenancePublisher
        )

    let private restorations (published: PoolCommand list ref) =
        published.Value |> List.filter (fun command -> command.OperationId.StartsWith "RESTORE#")

    [<Fact>]
    let ``a join is acknowledged only after its restore is enqueued, and a retried delivery restores once`` () =
        async {
            let! harness = removed HarnessOptions.Default
            let react = ReactToMembershipChanges(harness.CommandPublisher, harness.MaintenancePublisher)
            let! _ = harness.JoinAsync gamblerD
            harness.Fake.TakeChanges() |> ignore
            let created = [ MembershipCreated(pool, gambler, generation harness), "EVENT-JOIN" ]

            harness.Commands.FailSendWhen(fun message ->
                if message.Body.Contains "RESTORE_REJOIN_HISTORY" then Some(exn "SQS unavailable") else None)

            let! failed = react.ExecuteAsync created |> Async.Catch

            match failed with
            | Choice2Of2 _ -> ()
            | Choice1Of2() -> failwith "expected the enqueue failure to surface"

            harness.Commands.ClearSendFailures()
            do! react.ExecuteAsync created
            do! harness.DeliverCommandsAsync()
            total harness.Fake gamblerD |> shouldEqual (Some 14)

            // A duplicate stream delivery after the deduplication window adds nothing.
            harness.Commands.ExpireDeduplication()
            do! react.ExecuteAsync created
            do! harness.DeliverCommandsAsync()

            total harness.Fake gamblerD |> shouldEqual (Some 14)
            harness.Commands.DeadLetters |> shouldEqual []
        }

    [<Fact>]
    let ``reconciliation requests a missing restore for retained details of an earlier generation`` () =
        async {
            let! harness = rejoinedUnrestored HarnessOptions.Default
            let publisher, published = recordingPublisher ()

            let! report = (reconciliation harness publisher).ExecuteAsync(pool, None, "RECONCILE#1")

            report.RequestedRestorations |> shouldEqual 1
            restorations published |> shouldEqual [ restoreCommand harness ]

            do! harness.CommandPublisher.PublishAsync(restorations published)
            do! harness.RunUntilQuiescentAsync()
            total harness.Fake gamblerD |> shouldEqual (Some 14)

            // Once everything is restored, reconciliation requests nothing more.
            let publisher, published = recordingPublisher ()
            let! report = (reconciliation harness publisher).ExecuteAsync(pool, None, "RECONCILE#2")
            report.RequestedRestorations |> shouldEqual 0
            restorations published |> shouldEqual []
        }

    [<Fact>]
    let ``reconciliation requests nothing for a membership whose only unbound detail is held for repair`` () =
        async {
            let! harness = removed HarnessOptions.Default
            let malformed = detail harness.Fake gamblerD match2 |> Option.get
            malformed.Remove "score" |> ignore
            harness.Fake.Put("Pool", malformed)
            let! _ = harness.JoinAsync gamblerD
            do! harness.RunUntilQuiescentAsync()
            let publisher, published = recordingPublisher ()

            let! report = (reconciliation harness publisher).ExecuteAsync(pool, None, "RECONCILE#1")

            report.RequestedRestorations |> shouldEqual 0
            report.HeldRetainedDetails |> shouldEqual 1
            restorations published |> shouldEqual []
        }

    // 1.5 End-to-end outcome after propagation.

    let private rankedPage (harness: VersionedSettlementHarness) (matchId: string) =
        async {
            match! harness.Ranked.ExecuteAsync(Ulid.newOf gamblerA, pool, Ulid.newOf matchId, None) with
            | Ok page -> return List.ofSeq page.Items
            | Error failure -> return failwith $"unexpected {failure}"
        }

    [<Fact>]
    let ``propagation brings every restored detail to the final total, even after a higher earlier score version`` () =
        async {
            let! harness = removed HarnessOptions.Default

            for matchId in [ match1; match2 ] do
                let tombstone = detail harness.Fake gamblerD matchId |> Option.get
                tombstone["poolScoreVersion"] <- AttributeValue(N = "99")
                harness.Fake.Put("Pool", tombstone)

            let! _ = harness.JoinAsync gamblerD
            do! harness.RunUntilQuiescentAsync()

            for matchId in [ match1; match2 ] do
                assertBoundToCurrentTotal harness matchId
                detail harness.Fake gamblerD matchId |> number "totalPoolPoints" |> shouldEqual (Some 14)
        }

    [<Fact>]
    let ``the ranked list of an older match includes the rejoined gambler with the original prediction and award`` () =
        async {
            let! harness = rejoined HarnessOptions.Default
            do! harness.RunJobsAsync [ VerifyReadiness pool ]

            let! rows = rankedPage harness match1

            rows |> List.map _.GamblerId.Value |> shouldEqual [ gamblerA; gamblerB; gamblerD; gamblerC ]
            let restored = rows |> List.find (fun row -> row.GamblerId = gambler)

            restored.BetScore
            |> shouldEqual (Some { TeamScore.HomeTeamValue = BetScore.newOf 3; AwayTeamValue = BetScore.newOf 1 })

            restored.Score |> shouldEqual (Some 10)
        }

    [<Fact>]
    let ``a match scored during the absence follows the late-join rule`` () =
        async {
            let! harness = removed HarnessOptions.Default

            do!
                harness.HandleLayoutRecordsAsync
                    [ MatchInserted
                          { MatchId = match3
                            MatchDateTime = DateTime.UtcNow.AddDays 3.0
                            Result = None } ]

            do! harness.RunUntilQuiescentAsync()
            do! harness.SettleAsync(match3, 2, 0)

            let! _ = harness.JoinAsync gamblerD
            do! harness.RunUntilQuiescentAsync()

            total harness.Fake gamblerD |> shouldEqual (Some 14)
            isComputed harness.Fake gamblerD match3 |> shouldEqual true
            award harness.Fake gamblerD match3 |> shouldEqual (Some 0)
            detail harness.Fake gamblerD match3 |> has "homeTeamBet" |> shouldEqual false
        }

    [<Fact>]
    let ``a bet deleted at removal is not recreated and the open match can be predicted again`` () =
        async {
            let harness = VersionedSettlementHarness()
            do! harness.SeedAndMigrateAsync(defaultMembers, openMatches)
            let! _ = harness.JoinAsync gamblerD
            do! harness.RunUntilQuiescentAsync()
            do! predict harness gamblerD match1 (3, 1)
            do! predict harness gamblerD match2 (0, 2)
            do! harness.SettleAsync(match1, 1, 0)
            do! harness.RemoveAsync gamblerD
            do! harness.RunUntilQuiescentAsync()
            detail harness.Fake gamblerD match2 |> shouldEqual None

            let! _ = harness.JoinAsync gamblerD
            do! harness.RunUntilQuiescentAsync()

            total harness.Fake gamblerD |> shouldEqual (Some 10)
            isComputed harness.Fake gamblerD match2 |> shouldEqual false
            detail harness.Fake gamblerD match2 |> has "homeTeamBet" |> shouldEqual false

            do! predict harness gamblerD match2 (1, 1)
            detail harness.Fake gamblerD match2 |> number "homeTeamBet" |> shouldEqual (Some 1)
        }

    [<Fact>]
    let ``a correction after restoration changes the total by the award difference exactly once`` () =
        async {
            let! harness = rejoined HarnessOptions.Default

            // 1-0 → 0-1: the 3-1 prediction now earns 4 (away score) instead of 10.
            let! accepted = harness.CorrectResultAsync(match1, 0, 1)
            do! harness.RunUntilQuiescentAsync()

            accepted |> shouldEqual true
            total harness.Fake gamblerD |> shouldEqual (Some 8)
            award harness.Fake gamblerD match1 |> shouldEqual (Some 4)

            // A replayed dispatch of the same correction changes nothing.
            harness.Commands.ExpireDeduplication()
            let! _ = harness.Dispatch.ExecuteAsync(Ulid.newOf match1, { HomeTeamScore = 0; AwayTeamScore = 1; Version = 2 })
            do! harness.RunUntilQuiescentAsync()

            total harness.Fake gamblerD |> shouldEqual (Some 8)

            for matchId in [ match1; match2 ] do
                detail harness.Fake gamblerD matchId |> number "totalPoolPoints" |> shouldEqual (Some 8)
        }
