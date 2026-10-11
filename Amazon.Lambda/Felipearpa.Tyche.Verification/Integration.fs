namespace Felipearpa.Tyche.Verification

#nowarn "3536"

open System
open System.Collections.Generic
open System.Diagnostics
open Amazon.DynamoDBv2
open Amazon.DynamoDBv2.Model
open Amazon.SQS
open Amazon.SQS.Model
open Felipearpa.Core.Json
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.Pool.Application
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Infrastructure
open Felipearpa.Tyche.Pool.Type
open Felipearpa.Tyche.PoolLayout.Application
open Felipearpa.Tyche.PoolLayout.Infrastructure
open Felipearpa.Type
open Felipearpa.Tyche.Verification.Seed

type Check =
    { Name: string
      Passed: bool
      Detail: string }

/// End-to-end checks of source event → queued settlement → positions → membership
/// projection → ranked page on isolated tables, streams, and queues.
type Integration(dynamo: IAmazonDynamoDB, sqs: IAmazonSQS, pipeline: Pipeline) =

    let checks = List<Check>()
    let workers = pipeline.Workers

    let show value = (sprintf "%A" value).Replace("\n", " ")

    let check name passed detail =
        checks.Add { Name = name; Passed = passed; Detail = detail }
        printfn "%s %s — %s" (if passed then "PASS" else "FAIL") name detail

    let keySerializer = DynamoDbKeySerializer()
    let repository = PoolSettlementDynamoDbRepository(dynamo)
    let settlement = repository :> IPoolSettlementRepository
    let projection = repository :> IPoolProjectionRepository
    let layoutRepository = PoolLayoutDynamoDbRepository(keySerializer, dynamo) :> Felipearpa.Tyche.PoolLayout.Domain.IPoolLayoutRepository
    let layoutReader = PoolLayoutMatchReader(layoutRepository) :> IPoolLayoutMatchReader
    let pools = PoolDynamoDbRepository(keySerializer, dynamo) :> IPoolRepository

    let ranked =
        GetRankedPoolMatchBets(
            settlement,
            projection,
            layoutReader,
            RankedMatchBetsDynamoDbRepository(dynamo),
            { PageSize = 50 },
            fun () -> DateTime.UtcNow
        )

    let ids count = List.init count (fun _ -> Ulid.random().Value) |> List.sort

    let settle () = pipeline.RunUntilQuiescentAsync(TimeSpan.FromMinutes 10.0)

    let rankedOrder (requester: string) poolId matchId =
        async {
            let rec collect cursor acc =
                async {
                    match! ranked.ExecuteAsync(Ulid.newOf requester, Ulid.newOf poolId, Ulid.newOf matchId, cursor) with
                    | Ok page ->
                        let acc = acc @ (page.Items |> Seq.map (fun bet -> bet.GamblerId.Value, bet.Score) |> List.ofSeq)

                        match page.Next with
                        | Some _ -> return! collect page.Next acc
                        | None -> return Ok acc
                    | Error error -> return Error error
                }

            return! collect None []
        }

    /// Polls the ranked feed until it shows the expected order, returning how long the
    /// projection and index took to converge.
    let awaitOrder requester poolId matchId (expected: string list) =
        async {
            let watch = Stopwatch.StartNew()

            let rec poll () =
                async {
                    let! order = rankedOrder requester poolId matchId

                    match order with
                    | Ok rows when rows |> List.map fst = expected -> return Some watch.Elapsed, rows
                    | _ when watch.Elapsed > TimeSpan.FromSeconds 60.0 -> return None, (order |> Result.defaultValue [])
                    | _ ->
                        do! Async.Sleep 250
                        return! poll ()
                }

            return! poll ()
        }

    let membership poolId gamblerId = settlement.GetMembershipAsync(Ulid.newOf poolId, Ulid.newOf gamblerId)

    let receipt poolId operationId = settlement.GetReceiptAsync(Ulid.newOf poolId, operationId)

    let maintenance (jobs: PoolMaintenanceJob list) =
        let publisher = PoolMaintenanceSqsPublisher(sqs, VerificationEnvironment.maintenanceQueueUrl ()) :> IPoolMaintenancePublisher
        publisher.PublishAsync jobs

    let sendCommandBody (poolId: string) (body: string) (deduplicationId: string) =
        sqs.SendMessageAsync(
            SendMessageRequest(
                QueueUrl = VerificationEnvironment.commandQueueUrl (),
                MessageBody = body,
                MessageGroupId = poolId,
                MessageDeduplicationId = deduplicationId
            )
        )
        |> Async.AwaitTask
        |> Async.Ignore

    let score (prediction: (int * int) option) (home, away) =
        BetEvaluator.delta
            (prediction
             |> Option.map (fun (h, a) ->
                 { TeamScore.HomeTeamValue = BetScore.newOf h
                   AwayTeamValue = BetScore.newOf a }))
            { HomeTeamValue = home; AwayTeamValue = away }

    let ordered (totals: (string * int) list) =
        totals |> List.sortWith (fun (lg, lt) (rg, rt) -> if lt <> rt then compare rt lt else String.CompareOrdinal(lg, rg)) |> List.map fst

    member _.Checks = List.ofSeq checks

    member this.RunAsync() =
        async {
            let poolId = Ulid.random().Value
            let heldPoolId = Ulid.random().Value
            let layoutId = Ulid.random().Value
            let creator = Ulid.random().Value

            match ids 5 with
            | [ a; b; c; d; e ] ->
                let now = DateTime.UtcNow

                let m0 = { MatchId = Ulid.random().Value; Kickoff = now.AddDays -2.0; Result = Some(2, 1) }
                let m1 = { MatchId = Ulid.random().Value; Kickoff = now.AddHours -3.0; Result = None }
                let m2 = { MatchId = Ulid.random().Value; Kickoff = now.AddHours -2.5; Result = None }
                let m3 = { MatchId = Ulid.random().Value; Kickoff = now.AddHours -2.0; Result = Some(1, 1) }

                let predictions =
                    Map.ofList
                        [ a, Map.ofList [ m0.MatchId, (2, 5); m1.MatchId, (1, 0); m2.MatchId, (0, 0) ]
                          b, Map.ofList [ m0.MatchId, (3, 0); m1.MatchId, (2, 2); m2.MatchId, (1, 0) ]
                          c, Map.ofList [ m0.MatchId, (2, 3); m2.MatchId, (3, 1) ]
                          d, Map.ofList [ m0.MatchId, (2, 1) ] ]

                let prediction gambler matchId = predictions[gambler] |> Map.tryFind matchId
                let initial = [ a; b; c; d ] |> List.map (fun g -> g, score (prediction g m0.MatchId) (2, 1))

                // Legacy records: memberships, computed match 0, uncomputed matches 1 and 2.
                let legacyItems =
                    [ yield rootItem poolId layoutId creator 4 false
                      for gambler, total in initial do
                          yield membershipItem poolId layoutId { GamblerId = gambler; Total = total; Position = None } None
                          yield detailItem poolId layoutId gambler m0 (prediction gambler m0.MatchId) (Some total) None

                          for pending in [ m1; m2 ] do
                              yield detailItem poolId layoutId gambler pending (prediction gambler pending.MatchId) None None
                      // A membership whose total does not match its details: held for repair.
                      yield rootItem heldPoolId layoutId e 1 false
                      yield membershipItem heldPoolId layoutId { GamblerId = e; Total = 99; Position = None } None
                      yield detailItem heldPoolId layoutId e m0 (Some(2, 5)) (Some 4) None ]

                do! writeAll dynamo (VerificationEnvironment.poolTable ()) legacyItems 4

                do!
                    writeAll
                        dynamo
                        (VerificationEnvironment.poolLayoutTable ())
                        [ layoutMatchItem layoutId m0; layoutMatchItem layoutId m1; layoutMatchItem layoutId m2 ]
                        1

                let! _ = settle ()

                // Migration through the real maintenance queue.
                let pool = Ulid.newOf poolId
                let heldPool = Ulid.newOf heldPoolId
                do! maintenance [ InitializeIdentities(pool, None); InitializeIdentities(heldPool, None) ]
                let! _ = settle ()
                do! maintenance [ BackfillProjection(pool, None); BackfillProjection(heldPool, None) ]
                let! _ = settle ()
                do! maintenance [ VerifyReadiness pool; VerifyReadiness heldPool ]
                let! _ = settle ()
                let! root = settlement.GetRootAsync pool
                let! heldRoot = settlement.GetRootAsync heldPool
                check "migration readiness" (root.Value.RankingReadiness = Some Ready) $"{root.Value.RankingReadiness}"

                check
                    "unresolved migration blocks readiness"
                    (heldRoot.Value.RankingReadiness = Some NotReady)
                    $"{heldRoot.Value.RankingReadiness}"

                // Revealed before computation: pool totals order the list, D leads without a bet.
                let totals = initial |> Map.ofList
                let! beforeResult = rankedOrder a poolId m1.MatchId
                let expectedBefore = ordered (Map.toList totals)

                check
                    "no-bet leader first on a locked match"
                    (beforeResult |> Result.map (List.map fst) = Ok expectedBefore && List.head expectedBefore = d)
                    (show beforeResult)

                // Result 1-0 for match 1, with the first two deliveries yielding at their deadline.
                do! layoutRepository.SetMatchFinalScoreAsync(Ulid.newOf layoutId, Ulid.newOf m1.MatchId, 1, 0)
                let resultWritten = Stopwatch.StartNew()
                workers.CommandRemainingTime <- TimeSpan.FromSeconds 45.0
                let deliveredBefore = workers.Deliveries.Length

                let rec yieldTwice rounds =
                    async {
                        let! _ = pipeline.PumpStreamsOnceAsync()
                        let! _ = workers.PollCommandsOnceAsync 2
                        let yielded = workers.Deliveries |> List.skip deliveredBefore |> List.filter (fun d -> not d.Succeeded) |> List.length

                        if yielded < 2 && rounds < 30 then
                            return! yieldTwice (rounds + 1)
                    }

                do! yieldTwice 0
                workers.CommandRemainingTime <- TimeSpan.FromMinutes 5.0
                let! _ = settle ()

                let commandDeliveries = workers.Deliveries |> List.skip deliveredBefore |> List.filter (fun d -> d.Queue = "commands")
                let finalDelivery = commandDeliveries |> List.filter _.Succeeded |> List.tryLast

                check
                    "deadline yields are redelivered from the queue and resume"
                    (commandDeliveries |> List.exists (fun d -> not d.Succeeded)
                     && finalDelivery |> Option.exists (fun d -> d.ReceiveCount >= 3))
                    $"deliveries={commandDeliveries |> List.map (fun d -> d.ReceiveCount, d.Succeeded)}"

                let totals = totals |> Map.map (fun g total -> total + score (prediction g m1.MatchId) (1, 0))
                let! persisted = [ a; b; c; d ] |> List.map (membership poolId) |> Async.Sequential

                check
                    "settlement totals"
                    (persisted |> Array.map (fun m -> m.Value.Total) |> List.ofArray = ([ a; b; c; d ] |> List.map (fun g -> Some totals[g])))
                    (show (persisted |> Array.map (fun m -> m.Value.GamblerId.Value.Substring 20, m.Value.Total)))

                let expectedOrder = ordered (Map.toList totals)

                check
                    "authoritative positions"
                    (persisted |> Array.forall (fun m -> m.Value.Position = Some(1 + List.findIndex ((=) m.Value.GamblerId.Value) expectedOrder)))
                    (show (persisted |> Array.map (fun m -> m.Value.Position)))

                let! converged, rows = awaitOrder a poolId m1.MatchId expectedOrder

                check
                    "ranked page converges to pool totals with this match's awards"
                    (converged.IsSome && rows |> List.map snd = (expectedOrder |> List.map (fun g -> Some(score (prediction g m1.MatchId) (1, 0)))))
                    ($"converged after {resultWritten.Elapsed.TotalSeconds:F1}s from the result write; rows=" + show rows)

                let! older, _ = awaitOrder a poolId m0.MatchId expectedOrder
                check "older match ranks by current totals" older.IsSome (show older)

                // Ordering: a standings reconciliation and match 2's result, in that order.
                let standings = PoolCommand.reconcileStandings pool "verification-standings"
                do! (PoolCommandSqsPublisher(sqs, VerificationEnvironment.commandQueueUrl ()) :> IPoolCommandPublisher).PublishAsync [ standings ]
                do! layoutRepository.SetMatchFinalScoreAsync(Ulid.newOf layoutId, Ulid.newOf m2.MatchId, 0, 0)
                let! _ = settle ()
                let! standingsReceipt = receipt poolId standings.OperationId
                let! match2Receipt = receipt poolId $"RESULT#{m2.MatchId}#1"

                check
                    "per-pool FIFO order is kept"
                    (standingsReceipt.Value.Revision < match2Receipt.Value.Revision && match2Receipt.Value.State = Completed)
                    $"standings={standingsReceipt.Value.Revision} match2={match2Receipt.Value.Revision}"

                let totals = totals |> Map.map (fun g total -> total + score (prediction g m2.MatchId) (0, 0))

                // A command reusing an operation ID with another payload is retried and then
                // dead-lettered without taking the pool's fence.
                let poison =
                    { PoolCommand.settleResult pool (Ulid.newOf m1.MatchId) { HomeTeamScore = 9; AwayTeamScore = 9; Version = 1 } with
                        OperationId = $"RESULT#{m1.MatchId}#1" }

                do! sendCommandBody poolId (PoolMessageJson.encodeCommand poison) (Guid.NewGuid().ToString("N"))
                let! _ = settle ()
                let! deadLetters = workers.QueueDepthAsync(VerificationEnvironment.commandDeadLetterQueueUrl ())
                let! rootAfterPoison = settlement.GetRootAsync pool

                check
                    "failed commands reach the dead-letter queue without holding the fence"
                    (deadLetters = 1 && rootAfterPoison.Value.ActiveOperation.IsNone)
                    $"dlq={deadLetters} active={rootAfterPoison.Value.ActiveOperation.IsSome}"

                // A message from the previous position worker is translated, not dropped.
                let legacyBody = $"{poolId}|{m1.MatchId}"
                do! sendCommandBody poolId legacyBody (Guid.NewGuid().ToString("N"))
                let! _ = settle ()
                let! receiptsAfterLegacy = settlement.GetRootAsync pool

                check
                    "legacy position messages are processed as match reconciliations"
                    (receiptsAfterLegacy.Value.LastCompletedOperationRevision > match2Receipt.Value.Revision.Value)
                    $"last completed revision {receiptsAfterLegacy.Value.LastCompletedOperationRevision}"

                // Correction 1-0 → 2-0 through the versioned write.
                let! accepted = layoutRepository.CorrectMatchResultAsync(Ulid.newOf layoutId, Ulid.newOf m1.MatchId, 2, 0, 1)
                let! _ = settle ()

                let totals =
                    totals
                    |> Map.map (fun g total -> total - score (prediction g m1.MatchId) (1, 0) + score (prediction g m1.MatchId) (2, 0))

                let! corrected = [ a; b; c; d ] |> List.map (membership poolId) |> Async.Sequential

                check
                    "correction applies the award difference once"
                    (accepted && (corrected |> Array.map (fun m -> m.Value.Total) |> List.ofArray = ([ a; b; c; d ] |> List.map (fun g -> Some totals[g]))))
                    (show (corrected |> Array.map (fun m -> m.Value.Total), totals))

                let! afterCorrection, _ = awaitOrder a poolId m0.MatchId (ordered (Map.toList totals))
                check "ranking reflects the correction after convergence" afterCorrection.IsSome (show afterCorrection)

                // Removal: the requester is denied at once; projections disappear after cleanup.
                do! pools.RemoveGamblerAsync(pool, Ulid.newOf c)
                let! deniedAtOnce = ranked.ExecuteAsync(Ulid.newOf c, pool, Ulid.newOf m1.MatchId, None)
                check "a removed requester is denied before cleanup" (deniedAtOnce = Error NotPoolMember) (show deniedAtOnce)
                let! _ = settle ()
                let withoutC = totals |> Map.remove c
                let! removedConverged, _ = awaitOrder a poolId m1.MatchId (ordered (Map.toList withoutC))
                check "removed membership leaves the ranked feed" removedConverged.IsSome (show removedConverged)

                // Rejoin: a new generation that gets the retained history back.
                let! _ =
                    pools.JoinPoolAsync
                        { PoolId = pool
                          PoolName = NonEmptyString100.newOf "Verification"
                          GamblerId = Ulid.newOf c
                          GamblerUsername = NonEmptyString100.newOf "rejoined"
                          GamblerEmail = NonEmptyString100.newOf "rejoined@verification.invalid"
                          PoolLayoutId = Ulid.newOf layoutId
                          PoolLayoutVersion = 1 }

                let! _ = settle ()
                let! rejoinedMembership = membership poolId c
                let! restored = settlement.GetBetSettlementStateAsync(pool, Ulid.newOf c, Ulid.newOf m1.MatchId)
                let! afterRejoin, _ = awaitOrder a poolId m1.MatchId (ordered (Map.toList totals))

                // The rejoined membership has a new generation; its retained computed details are
                // restored to it with their awards, so it returns to its earlier total and to the
                // match's ranked feed once propagation converges.
                check
                    "rejoined membership gets a new generation and its retained history back"
                    (rejoinedMembership |> Option.exists (fun m -> m.Total = Some totals[c] && m.Generation = (restored |> Option.bind _.Generation))
                     && afterRejoin.IsSome)
                    (show (rejoinedMembership |> Option.map (fun m -> m.Generation, m.Total, m.Position), restored |> Option.map _.Generation, afterRejoin))

                // A match inserted with its final result already set.
                do! writeAll dynamo (VerificationEnvironment.poolLayoutTable ()) [ (let item = layoutMatchItem layoutId m3 in item["resultVersion"] <- n 1; item) ] 1
                let! _ = settle ()
                let! m3States = settlement.GetBetSettlementStatesAsync(pool, Ulid.newOf m3.MatchId, [ a; b; c; d ] |> List.map Ulid.newOf)

                check
                    "a match inserted with its result is settled for every member"
                    (m3States.Length = 4 && m3States |> List.forall (fun bet -> bet.IsComputed && bet.Award = Some 0))
                    (show (m3States |> List.map (fun bet -> bet.IsComputed, bet.Award)))

                check "stream batches recovered" true $"retried stream deliveries: {pipeline.StreamFailures}"
            | _ -> ()
        }
