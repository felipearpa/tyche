namespace Felipearpa.Tyche.Pool.Test.Settlement

open System
open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Test.Fakes
open Felipearpa.Tyche.Pool.Test.Settlement.PoolScenario
open Felipearpa.Type
open FsUnitTyped
open Xunit

/// Identity initialization, projection backfill, readiness, and reconciliation.
module MigrationAndReconciliationTest =

    let private pool = Ulid.newOf poolId

    let private key (pk: string) (sk: string) =
        Dictionary(dict [ "pk", AttributeValue(S = pk); "sk", AttributeValue(S = sk) ])

    let private delete (harness: VersionedSettlementHarness) (pk: string) (sk: string) =
        harness.Fake.Client.DeleteItemAsync(DeleteItemRequest(TableName = "Pool", Key = key pk sk)).Wait()

    let private legacyOnly () =
        let harness = VersionedSettlementHarness()
        seedLegacy harness.Fake defaultMembers defaultMatches
        harness

    [<Fact>]
    let ``identity initialization binds memberships and their details without changing totals or awards`` () =
        async {
            let harness = legacyOnly ()
            let totalsBefore = [ gamblerA; gamblerB; gamblerC ] |> List.map (total harness.Fake)

            do! harness.RunJobsAsync [ InitializeIdentities(pool, None) ]

            [ gamblerA; gamblerB; gamblerC ] |> List.map (total harness.Fake) |> shouldEqual totalsBefore

            for gamblerId in [ gamblerA; gamblerB; gamblerC ] do
                let generation = membership harness.Fake gamblerId |> text "membershipGeneration"
                generation |> Option.isSome |> shouldEqual true
                membership harness.Fake gamblerId |> number "scoreVersion" |> shouldEqual (Some 1)

                for matchId in [ match0; match1; match2 ] do
                    detail harness.Fake gamblerId matchId |> text "poolMembershipGeneration" |> shouldEqual generation

                detail harness.Fake gamblerId match0 |> number "resultVersion" |> shouldEqual (Some 1)
                detail harness.Fake gamblerId match1 |> has "resultVersion" |> shouldEqual false

            award harness.Fake gamblerB match0 |> shouldEqual (Some 10)
        }

    [<Fact>]
    let ``a membership whose details do not add up to its total is held for repair and keeps the pool not ready`` () =
        async {
            let harness = legacyOnly ()
            // A retained detail from an earlier membership of B makes B's details exceed its total.
            let current = membership harness.Fake gamblerB |> Option.get
            current["score"] <- AttributeValue(N = "3")
            harness.Fake.Put("Pool", current)

            do! harness.RunJobsAsync [ InitializeIdentities(pool, None) ]
            do! harness.RunJobsAsync [ BackfillProjection(pool, None) ]
            do! harness.RunJobsAsync [ VerifyReadiness pool ]

            harness.IdentityReports |> List.collect _.HeldMembers |> shouldEqual [ Ulid.newOf gamblerB ]
            detail harness.Fake gamblerB match1 |> has "poolMembershipGeneration" |> shouldEqual false
            detail harness.Fake gamblerA match1 |> has "poolMembershipGeneration" |> shouldEqual true

            let report = harness.ReadinessReports |> List.last |> Option.get
            report.Readiness |> shouldEqual NotReady
            report.Unresolved |> shouldEqual 3
        }

    [<Fact>]
    let ``backfill covers every eligible detail, is restartable, and makes the pool ready`` () =
        async {
            let harness = legacyOnly ()
            do! harness.RunJobsAsync [ InitializeIdentities(pool, None) ]
            do! harness.RunJobsAsync [ BackfillProjection(pool, None) ]
            let afterFirst = harness.Fake.Items "Pool" |> List.length
            do! harness.RunJobsAsync [ BackfillProjection(pool, None) ]
            do! harness.RunJobsAsync [ VerifyReadiness pool ]

            harness.Fake.Items "Pool" |> List.length |> shouldEqual afterFirst

            for gamblerId, expected in [ gamblerA, 4; gamblerB, 10; gamblerC, 6 ] do
                for matchId in [ match0; match1; match2 ] do
                    detail harness.Fake gamblerId matchId |> number "totalPoolPoints" |> shouldEqual (Some expected)

            let report = harness.ReadinessReports |> List.last |> Option.get
            report.Readiness |> shouldEqual Ready
            report.CheckedDetails |> shouldEqual 9
            root harness.Fake |> Option.map (fun item -> item["rankingReadiness"].M["state"].S) |> shouldEqual (Some "READY")
        }

    [<Fact>]
    let ``backfill that overlaps a total change retries with the newer total`` () =
        async {
            let harness = legacyOnly ()
            do! harness.RunJobsAsync [ InitializeIdentities(pool, None) ]
            let raced = ref false

            harness.Fake.Intercept(fun request ->
                match request with
                | :? TransactWriteItemsRequest as transaction when
                    not raced.Value
                    && transaction.TransactItems
                       |> Seq.exists (fun item -> not (isNull item.Update) && item.Update.UpdateExpression.Contains "#rankingActive")
                    ->
                    raced.Value <- true
                    let current = membership harness.Fake gamblerA |> Option.get
                    current["score"] <- AttributeValue(N = "50")
                    current["scoreVersion"] <- AttributeValue(N = "2")
                    harness.Fake.Put("Pool", current)
                    Proceed
                | _ -> Proceed)

            do! harness.RunJobsAsync [ BackfillProjection(pool, None) ]

            raced.Value |> shouldEqual true

            for matchId in [ match0; match1; match2 ] do
                detail harness.Fake gamblerA matchId |> number "totalPoolPoints" |> shouldEqual (Some 50)
                detail harness.Fake gamblerA matchId |> number "poolScoreVersion" |> shouldEqual (Some 2)
        }

    [<Fact>]
    let ``readiness is not declared when the membership set changes during the check`` () =
        async {
            let! harness =
                async {
                    let harness = VersionedSettlementHarness()
                    do! harness.SeedAndMigrateAsync(defaultMembers, defaultMatches)
                    return harness
                }

            let joined = ref false

            harness.Fake.Intercept(fun request ->
                match request with
                | :? UpdateItemRequest as update when not joined.Value && update.UpdateExpression.Contains "#rankingReadiness" ->
                    joined.Value <- true
                    harness.JoinAsync gamblerD |> Async.RunSynchronously |> ignore
                    Proceed
                | _ -> Proceed)

            do! harness.RunJobsAsync [ VerifyReadiness pool ]

            joined.Value |> shouldEqual true
            (harness.ReadinessReports |> List.head |> Option.get).Readiness |> shouldEqual NotReady
        }

    [<Fact>]
    let ``reconciliation recovers missing details, unsettled matches, stale projections, and positions`` () =
        async {
            let! harness =
                async {
                    let harness = VersionedSettlementHarness()
                    do! harness.SeedAndMigrateAsync(defaultMembers, defaultMatches)
                    return harness
                }

            do! harness.SettleAsync(match1, 1, 0)

            // Lost work: a detail never materialized, a result never dispatched, a projection
            // and a position never written.
            delete harness $"GAMBLER#{gamblerC}#POOL#{poolId}" $"MATCH#{match2}"
            harness.SetLayoutResult(match2, 0, 0)
            let staleDetail = detail harness.Fake gamblerB match0 |> Option.get
            staleDetail["poolScoreVersion"] <- AttributeValue(N = "0")
            staleDetail["totalPoolPoints"] <- AttributeValue(N = "1")
            harness.Fake.Put("Pool", staleDetail)
            let stalePosition = membership harness.Fake gamblerC |> Option.get
            stalePosition["positionOperationRevision"] <- AttributeValue(N = "0")
            harness.Fake.Put("Pool", stalePosition)

            do! harness.RunJobsAsync [ ReconcilePool(pool, None) ]

            [ gamblerA; gamblerB; gamblerC ]
            |> List.map (fun gamblerId -> isComputed harness.Fake gamblerId match2)
            |> shouldEqual [ true; true; true ]

            [ total harness.Fake gamblerA; total harness.Fake gamblerB; total harness.Fake gamblerC ] |> shouldEqual [ Some 44; Some 14; Some 6 ]
            detail harness.Fake gamblerB match0 |> number "totalPoolPoints" |> shouldEqual (Some 14)
            detail harness.Fake gamblerC match2 |> has "homeTeamBet" |> shouldEqual false

            membership harness.Fake gamblerC |> number "positionOperationRevision"
            |> shouldEqual (root harness.Fake |> number "lastCompletedOperationRevision")
        }

    [<Fact>]
    let ``reconciliation retires active details of an earlier generation`` () =
        async {
            let! harness =
                async {
                    let harness = VersionedSettlementHarness()
                    do! harness.SeedAndMigrateAsync(defaultMembers, defaultMatches)
                    return harness
                }

            // An uncomputed detail: a computed one of an earlier generation is retained history,
            // which reconciliation hands to the rejoin restoration instead.
            let stale = detail harness.Fake gamblerA match1 |> Option.get
            stale["poolMembershipGeneration"] <- AttributeValue(S = "OLD-GENERATION")
            harness.Fake.Put("Pool", stale)

            do! harness.RunJobsAsync [ ReconcilePool(pool, None) ]

            detail harness.Fake gamblerA match1 |> has "getPoolMatchBetsByTotalSk" |> shouldEqual false
            detail harness.Fake gamblerA match1 |> text "poolMembershipGeneration" |> shouldEqual (Some "OLD-GENERATION")
            detail harness.Fake gamblerA match1 |> number "homeTeamBet" |> shouldEqual (Some 1)
            total harness.Fake gamblerA |> shouldEqual (Some 4)
        }

    [<Fact>]
    let ``migration writes that meet transaction contention are retried until bound and active`` () =
        async {
            let harness = legacyOnly ()
            let conflicts = ref 0

            // Every other transaction is cancelled by contention, as back-to-back transactions on
            // one membership can be in DynamoDB.
            harness.Fake.Intercept(fun request ->
                match request with
                | :? TransactWriteItemsRequest ->
                    conflicts.Value <- conflicts.Value + 1

                    if conflicts.Value % 2 = 1 then
                        FailBefore(
                            TransactionCanceledException(
                                "Transaction cancelled [None, TransactionConflict]",
                                CancellationReasons =
                                    List([ CancellationReason(Code = "None"); CancellationReason(Code = "TransactionConflict") ])
                            )
                        )
                    else
                        Proceed
                | _ -> Proceed)

            do! harness.RunJobsAsync [ InitializeIdentities(pool, None) ]
            do! harness.RunJobsAsync [ BackfillProjection(pool, None) ]
            harness.Fake.ClearInterception()
            do! harness.RunJobsAsync [ VerifyReadiness pool ]

            conflicts.Value > 10 |> shouldEqual true
            (harness.ReadinessReports |> List.last |> Option.get).Readiness |> shouldEqual Ready
        }
