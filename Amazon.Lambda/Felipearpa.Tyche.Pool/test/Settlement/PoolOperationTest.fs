namespace Felipearpa.Tyche.Pool.Test.Settlement

open System
open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Tyche.Pool.Application
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Infrastructure
open Felipearpa.Tyche.Pool.Test.Fakes
open Felipearpa.Tyche.Pool.Test.Settlement.PoolScenario
open Felipearpa.Type
open FsUnitTyped
open Xunit

/// Admission, receipts, checkpoints, ownership, yields, assistance, and completion of
/// ordered pool operations.
module PoolOperationTest =

    let private pool = Ulid.newOf poolId

    let private result home away version =
        { HomeTeamScore = home
          AwayTeamScore = away
          Version = version }

    let private migrated (options: HarnessOptions) =
        async {
            let harness = VersionedSettlementHarness(options)
            do! harness.SeedAndMigrateAsync(defaultMembers, defaultMatches)
            return harness
        }

    let private setResult (harness: VersionedSettlementHarness) matchId home away =
        harness.SetLayoutResult(matchId, home, away)

    let private settle (harness: VersionedSettlementHarness) matchId home away =
        async {
            setResult harness matchId home away
            do! harness.CommandPublisher.PublishAsync [ PoolCommand.settleResult pool (Ulid.newOf matchId) (result home away 1) ]
            do! harness.RunUntilQuiescentAsync()
        }

    let private receipt (harness: VersionedSettlementHarness) operationId =
        harness.Fake.TryGet(
            "Pool",
            dict [ "pk", AttributeValue(S = $"POOL#{poolId}"); "sk", AttributeValue(S = $"SETTLEMENT#{operationId}") ]
        )

    let private assertPositions (fake: FakeDynamoDb) =
        [ position fake gamblerA; position fake gamblerB; position fake gamblerC ] |> shouldEqual [ Some 1; Some 2; Some 3 ]
        [ beforePosition fake gamblerA; beforePosition fake gamblerB; beforePosition fake gamblerC ] |> shouldEqual [ Some 3; Some 1; Some 2 ]

    [<Fact>]
    let ``a duplicate command is a no-op after completion and keeps its receipt`` () =
        async {
            let! harness = migrated HarnessOptions.Default
            do! settle harness match1 1 0

            let command = PoolCommand.settleResult pool (Ulid.newOf match1) (result 1 0 1)
            let! report = harness.ProcessCommand.ExecuteAsync(command, DateTime.UtcNow.AddMinutes 5.0)

            report.Outcome |> shouldEqual PoolCommandOutcome.AlreadyCompleted
            total harness.Fake gamblerA |> shouldEqual (Some 24)
            receipt harness command.OperationId |> text "operationState" |> shouldEqual (Some "COMPLETED")
            receipt harness command.OperationId |> number "operationRevision" |> shouldEqual (Some 1)
            receipt harness command.OperationId |> has "score" |> shouldEqual false
        }

    [<Fact>]
    let ``reusing an operation ID with another payload is an error`` () =
        async {
            let! harness = migrated HarnessOptions.Default
            do! settle harness match1 1 0

            let conflicting =
                { PoolCommand.settleResult pool (Ulid.newOf match1) (result 3 3 1) with
                    OperationId = $"RESULT#{match1}#1" }

            let! outcome = harness.ProcessCommand.ExecuteAsync(conflicting, DateTime.UtcNow.AddMinutes 5.0) |> Async.Catch

            match outcome with
            | Choice2Of2(:? InvalidOperationException) -> ()
            | other -> failwith $"expected a payload mismatch, got {other}"

            total harness.Fake gamblerA |> shouldEqual (Some 24)
        }

    [<Fact>]
    let ``replaying operation A after B and a new join changes nothing`` () =
        async {
            let! harness = migrated HarnessOptions.Default
            do! settle harness match1 1 0
            do! settle harness match2 0 0
            let! _ = harness.JoinAsync gamblerD
            do! harness.RunUntilQuiescentAsync()

            let before = [ gamblerA; gamblerB; gamblerC; gamblerD ] |> List.map (fun g -> position harness.Fake g, beforePosition harness.Fake g)
            let match1Positions = [ gamblerA; gamblerB; gamblerC ] |> List.map (fun g -> detailPosition harness.Fake g match1, detailBeforePosition harness.Fake g match1)

            let replay = PoolCommand.settleResult pool (Ulid.newOf match1) (result 1 0 1)
            let! report = harness.ProcessCommand.ExecuteAsync(replay, DateTime.UtcNow.AddMinutes 5.0)

            report.Outcome |> shouldEqual PoolCommandOutcome.AlreadyCompleted
            [ gamblerA; gamblerB; gamblerC; gamblerD ] |> List.map (fun g -> position harness.Fake g, beforePosition harness.Fake g) |> shouldEqual before

            [ gamblerA; gamblerB; gamblerC ]
            |> List.map (fun g -> detailPosition harness.Fake g match1, detailBeforePosition harness.Fake g match1)
            |> shouldEqual match1Positions

            detail harness.Fake gamblerD match1 |> has "position" |> shouldEqual false
        }

    [<Fact>]
    let ``an operation yields at the deadline, keeps its message, and resumes from its checkpoint`` () =
        async {
            let options =
                { HarnessOptions.Default with
                    Settings = { PoolOperationSettings.Default with MembershipPageSize = 1 }
                    Tick = TimeSpan.FromSeconds 1.0
                    InvocationBudget = TimeSpan.FromSeconds 6.0 }

            let! harness = migrated options
            setResult harness match1 1 0

            do!
                harness.CommandPublisher.PublishAsync
                    [ PoolCommand.settleResult pool (Ulid.newOf match1) (result 1 0 1) ]

            do! harness.RunUntilQuiescentAsync()

            let outcomes = harness.CommandOutcomes |> List.map _.Outcome
            outcomes |> List.contains PoolCommandOutcome.Yielded |> shouldEqual true
            outcomes |> List.last |> shouldEqual PoolCommandOutcome.OperationCompleted
            [ total harness.Fake gamblerA; total harness.Fake gamblerB; total harness.Fake gamblerC ] |> shouldEqual [ Some 24; Some 10; Some 6 ]
            position harness.Fake gamblerA |> shouldEqual (Some 1)
            beforePosition harness.Fake gamblerA |> shouldEqual (Some 3)
            harness.Commands.DeadLetters |> shouldEqual []
        }

    [<Fact>]
    let ``a worker that lost ownership cannot save its checkpoint or complete`` () =
        async {
            let! harness = migrated HarnessOptions.Default
            setResult harness match1 1 0
            let root = (harness.Settlement.GetRootAsync pool |> Async.RunSynchronously).Value
            let command = PoolCommand.settleResult pool (Ulid.newOf match1) (result 1 0 1)

            let stale =
                { OperationId = command.OperationId
                  Revision = 1L
                  Target = MatchSettlement(Ulid.newOf match1, result 1 0 1)
                  Stage = Scoring
                  Cursor = None
                  CalculationRevision = 1
                  MembershipRevision = root.MembershipRevision
                  OwnerToken = "stale-owner"
                  LeaseExpiresAt = DateTime.UtcNow.AddMinutes -1.0
                  AttemptsWithoutProgress = 0
                  Blocked = false
                  UnresolvedDetails = 0
                  AdmittedAt = DateTime.UtcNow
                  LastProgressAt = DateTime.UtcNow }

            let receiptOf state =
                { OperationId = command.OperationId
                  Revision = Some 1L
                  Fingerprint = PoolCommand.fingerprint command
                  State = state
                  Command = command
                  DeferredAt = None }

            let! admitted = harness.Settlement.AdmitAsync(root, receiptOf Admitted, stale)
            admitted |> shouldEqual Written

            // A redelivery claims the expired lease and finishes the operation.
            let! report = harness.ProcessCommand.ExecuteAsync(command, DateTime.UtcNow.AddMinutes 5.0)
            report.Outcome |> shouldEqual PoolCommandOutcome.OperationCompleted

            let! saved = harness.Settlement.ReplaceCheckpointAsync(pool, "stale-owner", { stale with Cursor = Some "x" })
            let! completed = harness.Settlement.CompleteAsync(pool, stale, 0)
            saved |> shouldEqual ConditionNotMet
            completed |> shouldEqual ConditionNotMet
            total harness.Fake gamblerA |> shouldEqual (Some 24)
        }

    [<Fact>]
    let ``a delivery that meets a live lease waits for that lease instead of claiming`` () =
        async {
            let! harness = migrated HarnessOptions.Default
            setResult harness match1 1 0
            let settlementRoot = (harness.Settlement.GetRootAsync pool |> Async.RunSynchronously).Value
            let command = PoolCommand.settleResult pool (Ulid.newOf match1) (result 1 0 1)

            let held =
                { OperationId = command.OperationId
                  Revision = 1L
                  Target = MatchSettlement(Ulid.newOf match1, result 1 0 1)
                  Stage = Scoring
                  Cursor = None
                  CalculationRevision = 1
                  MembershipRevision = settlementRoot.MembershipRevision
                  OwnerToken = "other-worker"
                  LeaseExpiresAt = harness.Now().AddMinutes 4.0
                  AttemptsWithoutProgress = 0
                  Blocked = false
                  UnresolvedDetails = 0
                  AdmittedAt = harness.Now()
                  LastProgressAt = harness.Now() }

            let receipt =
                { OperationId = command.OperationId
                  Revision = Some 1L
                  Fingerprint = PoolCommand.fingerprint command
                  State = Admitted
                  Command = command
                  DeferredAt = None }

            let! admitted = harness.Settlement.AdmitAsync(settlementRoot, receipt, held)
            admitted |> shouldEqual Written

            let! report = harness.ProcessCommand.ExecuteAsync(command, harness.Now().AddMinutes 5.0)

            report.Outcome |> shouldEqual PoolCommandOutcome.Yielded
            let wait = report.RetryAfter |> Option.get
            wait |> shouldBeGreaterThan (TimeSpan.FromMinutes 3.9)
            wait |> shouldBeSmallerThan (TimeSpan.FromMinutes 4.0 + TimeSpan.FromSeconds 1.0)
            // Nothing was claimed or scored while the other worker holds the lease.
            let rootItem = root harness.Fake |> Option.get
            rootItem["activeOperation"].M["ownerToken"].S |> shouldEqual "other-worker"
            total harness.Fake gamblerA |> shouldEqual (Some 4)
        }

    [<Fact>]
    let ``a later command helps finish an operation stuck after its message exhausted retries`` () =
        async {
            let options = { HarnessOptions.Default with CommandRedriveLimit = 2 }
            let! harness = migrated options
            setResult harness match1 1 0
            setResult harness match2 0 0

            let failing = ref true
            let detailKey = $"GAMBLER#{gamblerC}#POOL#{poolId}"

            harness.Fake.Intercept(fun request ->
                match request with
                | :? TransactWriteItemsRequest as transaction when
                    failing.Value
                    && transaction.TransactItems
                       |> Seq.exists (fun item ->
                           not (isNull item.Update)
                           && item.Update.Key["pk"].S = detailKey
                           && item.Update.Key["sk"].S = $"MATCH#{match1}"
                           && item.Update.UpdateExpression.Contains "computedRequestId")
                    ->
                    FailBefore(InternalServerErrorException "Internal server error")
                | _ -> Proceed)

            do! harness.CommandPublisher.PublishAsync [ PoolCommand.settleResult pool (Ulid.newOf match1) (result 1 0 1) ]
            do! harness.DeliverCommandsAsync()
            harness.Commands.DeadLetters.Length |> shouldEqual 1
            (root harness.Fake |> Option.get).ContainsKey "activeOperation" |> shouldEqual true

            failing.Value <- false
            do! harness.CommandPublisher.PublishAsync [ PoolCommand.settleResult pool (Ulid.newOf match2) (result 0 0 1) ]
            do! harness.RunUntilQuiescentAsync()

            receipt harness $"RESULT#{match1}#1" |> text "operationState" |> shouldEqual (Some "COMPLETED")
            receipt harness $"RESULT#{match2}#1" |> text "operationState" |> shouldEqual (Some "COMPLETED")
            receipt harness $"RESULT#{match1}#1" |> number "operationRevision" |> shouldEqual (Some 1)
            receipt harness $"RESULT#{match2}#1" |> number "operationRevision" |> shouldEqual (Some 2)
            [ total harness.Fake gamblerA; total harness.Fake gamblerB; total harness.Fake gamblerC ] |> shouldEqual [ Some 44; Some 14; Some 6 ]
            membership harness.Fake gamblerA |> number "positionOperationRevision" |> shouldEqual (Some 2)
        }

    [<Fact>]
    let ``a blocked pool defers later commands and resumes them after operator repair`` () =
        async {
            let options =
                { HarnessOptions.Default with
                    CommandRedriveLimit = 10
                    Settings = { PoolOperationSettings.Default with AttemptsBeforeBlocked = 2 } }

            let! harness = migrated options
            setResult harness match1 1 0
            setResult harness match2 0 0
            let failing = ref true

            harness.Fake.Intercept(fun request ->
                match request with
                | :? TransactWriteItemsRequest as transaction when
                    failing.Value
                    && transaction.TransactItems
                       |> Seq.exists (fun item ->
                           not (isNull item.Update)
                           && item.Update.Key["sk"].S = $"MATCH#{match1}"
                           && item.Update.UpdateExpression.Contains "computedRequestId")
                    ->
                    FailBefore(InternalServerErrorException "Internal server error")
                | _ -> Proceed)

            do! harness.CommandPublisher.PublishAsync [ PoolCommand.settleResult pool (Ulid.newOf match1) (result 1 0 1) ]
            do! harness.DeliverCommandsAsync()
            harness.Commands.DeadLetters.Length |> shouldEqual 1
            harness.CommandOutcomes |> List.map _.Outcome |> List.contains PoolCommandOutcome.Blocked |> shouldEqual true

            // A later command does not cascade into the dead-letter queue; it is held.
            do! harness.CommandPublisher.PublishAsync [ PoolCommand.settleResult pool (Ulid.newOf match2) (result 0 0 1) ]
            do! harness.DeliverCommandsAsync()
            harness.Commands.DeadLetters.Length |> shouldEqual 1
            receipt harness $"RESULT#{match2}#1" |> text "operationState" |> shouldEqual (Some "DEFERRED")
            total harness.Fake gamblerA |> shouldEqual (Some 4)

            // Operator repair: fix the cause, unblock the checkpoint, redrive the dead letter.
            failing.Value <- false
            let active = (harness.Settlement.GetRootAsync pool |> Async.RunSynchronously).Value.ActiveOperation.Value
            let! unblocked = harness.Settlement.ReplaceCheckpointAsync(pool, active.OwnerToken, { active with Blocked = false; AttemptsWithoutProgress = 0 })
            unblocked |> shouldEqual Written
            harness.Commands.Redrive()
            do! harness.RunUntilQuiescentAsync()

            receipt harness $"RESULT#{match1}#1" |> text "operationState" |> shouldEqual (Some "COMPLETED")
            receipt harness $"RESULT#{match2}#1" |> text "operationState" |> shouldEqual (Some "COMPLETED")
            [ total harness.Fake gamblerA; total harness.Fake gamblerB; total harness.Fake gamblerC ] |> shouldEqual [ Some 44; Some 14; Some 6 ]
        }

    [<Fact>]
    let ``completion is atomic: a failed completion keeps the fence and the next delivery completes`` () =
        async {
            let! harness = migrated HarnessOptions.Default
            setResult harness match1 1 0
            let failed = ref false

            harness.Fake.Intercept(fun request ->
                match request with
                | :? TransactWriteItemsRequest as transaction when
                    not failed.Value
                    && transaction.TransactItems
                       |> Seq.exists (fun item ->
                           not (isNull item.Update) && item.Update.UpdateExpression.StartsWith "REMOVE #activeOperation")
                    ->
                    failed.Value <- true
                    FailBefore(InternalServerErrorException "Internal server error")
                | _ -> Proceed)

            do! harness.CommandPublisher.PublishAsync [ PoolCommand.settleResult pool (Ulid.newOf match1) (result 1 0 1) ]
            do! harness.RunUntilQuiescentAsync()

            failed.Value |> shouldEqual true
            receipt harness $"RESULT#{match1}#1" |> text "operationState" |> shouldEqual (Some "COMPLETED")
            (root harness.Fake |> Option.get).ContainsKey "activeOperation" |> shouldEqual false
            total harness.Fake gamblerA |> shouldEqual (Some 24)
        }

    [<Fact>]
    let ``a contended completion is tried again and completes on the same delivery`` () =
        async {
            let! harness = migrated HarnessOptions.Default
            setResult harness match1 1 0
            let contended = ref false

            harness.Fake.Intercept(fun request ->
                match request with
                | :? TransactWriteItemsRequest as transaction when
                    not contended.Value
                    && transaction.TransactItems
                       |> Seq.exists (fun item ->
                           not (isNull item.Update) && item.Update.UpdateExpression.StartsWith "REMOVE #activeOperation")
                    ->
                    contended.Value <- true
                    // A join committed on the pool root at the same moment.
                    FailBefore(TransactionConflictException "Transaction is ongoing for the item")
                | _ -> Proceed)

            let outcomesBefore = harness.CommandOutcomes.Length
            do! harness.CommandPublisher.PublishAsync [ PoolCommand.settleResult pool (Ulid.newOf match1) (result 1 0 1) ]
            do! harness.RunUntilQuiescentAsync()

            contended.Value |> shouldEqual true

            harness.CommandOutcomes
            |> List.skip outcomesBefore
            |> List.map _.Outcome
            |> shouldEqual [ PoolCommandOutcome.OperationCompleted ]

            receipt harness $"RESULT#{match1}#1" |> text "operationState" |> shouldEqual (Some "COMPLETED")
            (root harness.Fake |> Option.get).ContainsKey "activeOperation" |> shouldEqual false
            harness.Commands.DeadLetters |> shouldEqual []
            total harness.Fake gamblerA |> shouldEqual (Some 24)
        }

    [<Fact>]
    let ``the pool root keeps a fixed set of attributes however many operations complete`` () =
        async {
            let! harness = migrated HarnessOptions.Default
            let! _ = harness.JoinAsync gamblerD
            do! harness.RunUntilQuiescentAsync()
            let afterOne = (root harness.Fake |> Option.get).Keys |> Set.ofSeq
            do! settle harness match1 1 0
            do! settle harness match2 0 0
            do! harness.RemoveAsync gamblerD
            do! harness.RunUntilQuiescentAsync()
            let afterMany = (root harness.Fake |> Option.get).Keys |> Set.ofSeq

            afterMany |> shouldEqual afterOne
            root harness.Fake |> number "lastCompletedOperationRevision" |> shouldEqual (Some 4)
        }

    [<Fact>]
    let ``zero awards finalize the detail without a new score version`` () =
        async {
            let! harness = migrated HarnessOptions.Default
            let versionBefore = membership harness.Fake gamblerC |> number "scoreVersion"
            do! settle harness match1 1 0

            isComputed harness.Fake gamblerC match1 |> shouldEqual true
            award harness.Fake gamblerC match1 |> shouldEqual (Some 0)
            membership harness.Fake gamblerC |> number "scoreVersion" |> shouldEqual versionBefore
            membership harness.Fake gamblerA |> number "scoreVersion" |> shouldEqual (versionBefore |> Option.map ((+) 1))
            membership harness.Fake gamblerC |> number "beforeScore" |> shouldEqual (Some 6)
        }

    [<Fact>]
    let ``a missing detail is left pending, never upserted or settled as no bet`` () =
        async {
            let! harness = migrated HarnessOptions.Default

            harness.Fake.Client.DeleteItemAsync(
                DeleteItemRequest(
                    TableName = "Pool",
                    Key =
                        Dictionary(
                            dict
                                [ "pk", AttributeValue(S = $"GAMBLER#{gamblerB}#POOL#{poolId}")
                                  "sk", AttributeValue(S = $"MATCH#{match1}") ]
                        )
                )
            ).Wait()

            do! settle harness match1 1 0

            detail harness.Fake gamblerB match1 |> shouldEqual None
            receipt harness $"RESULT#{match1}#1" |> number "unresolvedDetails" |> shouldEqual (Some 1)
            total harness.Fake gamblerB |> shouldEqual (Some 10)
            total harness.Fake gamblerA |> shouldEqual (Some 24)
        }

    [<Fact>]
    let ``a membership removed before settlement is excluded and not recreated`` () =
        async {
            let! harness = migrated HarnessOptions.Default
            do! harness.RemoveAsync gamblerC
            setResult harness match2 0 0

            do! harness.CommandPublisher.PublishAsync [ PoolCommand.settleResult pool (Ulid.newOf match2) (result 0 0 1) ]
            do! harness.DeliverCommandsAsync()

            membership harness.Fake gamblerC |> shouldEqual None
            total harness.Fake gamblerA |> shouldEqual (Some 24)
        }

    [<Fact>]
    let ``a join during the position pass invalidates it and the new member is ranked`` () =
        async {
            let! harness = migrated HarnessOptions.Default
            setResult harness match1 1 0
            let joined = ref false

            harness.Fake.Intercept(fun request ->
                match request with
                | :? TransactWriteItemsRequest as transaction when
                    not joined.Value
                    && transaction.TransactItems
                       |> Seq.exists (fun item -> not (isNull item.Update) && item.Update.UpdateExpression.Contains "#positionCalculationRevision")
                    ->
                    joined.Value <- true
                    // A concurrent join commits while positions are being written.
                    harness.JoinAsync gamblerD |> Async.RunSynchronously |> ignore
                    Proceed
                | _ -> Proceed)

            do! harness.CommandPublisher.PublishAsync [ PoolCommand.settleResult pool (Ulid.newOf match1) (result 1 0 1) ]
            do! harness.DeliverCommandsAsync()

            joined.Value |> shouldEqual true
            position harness.Fake gamblerD |> shouldEqual (Some 4)
            receipt harness $"RESULT#{match1}#1" |> text "operationState" |> shouldEqual (Some "COMPLETED")
            assertPositions harness.Fake
        }

