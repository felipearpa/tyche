namespace Felipearpa.Tyche.Verification

#nowarn "3536"

open System
open System.Collections.Generic
open System.Diagnostics
open System.Threading
open Amazon.DynamoDBv2
open Amazon.SQS
open Felipearpa.Core.Json
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.Pool.Application
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Infrastructure
open Felipearpa.Tyche.PoolLayout.Application
open Felipearpa.Tyche.PoolLayout.Infrastructure
open Felipearpa.Type
open Felipearpa.Tyche.Verification.Seed

type LoadMeasurement = { Name: string; Value: string }

/// Settlement, finalization, and projection writes alongside ranked reads for a pool of
/// thousands of members with many matches each, on isolated resources.
type Load(dynamo: IAmazonDynamoDB, sqs: IAmazonSQS, pipeline: Pipeline, members: int, matches: int, viewers: int) =

    let measurements = List<LoadMeasurement>()
    let workers = pipeline.Workers

    let measure name value =
        measurements.Add { Name = name; Value = value }
        printfn "MEASURE %s = %s" name value

    let keySerializer = DynamoDbKeySerializer()
    let repository = PoolSettlementDynamoDbRepository(dynamo)
    let settlement = repository :> IPoolSettlementRepository
    let projection = repository :> IPoolProjectionRepository
    let layoutRepository = PoolLayoutDynamoDbRepository(keySerializer, dynamo) :> Felipearpa.Tyche.PoolLayout.Domain.IPoolLayoutRepository
    let layoutReader = PoolLayoutMatchReader(layoutRepository) :> IPoolLayoutMatchReader

    let ranked =
        GetRankedPoolMatchBets(
            settlement,
            projection,
            layoutReader,
            RankedMatchBetsDynamoDbRepository(dynamo),
            { PageSize = 50 },
            fun () -> DateTime.UtcNow
        )

    let percentile (p: float) (values: float list) =
        match values |> List.sort with
        | [] -> nan
        | sorted -> sorted[min (sorted.Length - 1) (int (ceil (p * float sorted.Length)) - 1)]

    let summarize name (latencies: float list) (failures: int) =
        measure
            name
            $"requests={latencies.Length + failures} failures={failures} p50={percentile 0.5 latencies:F0}ms p95={percentile 0.95 latencies:F0}ms max={List.max (0.0 :: latencies):F0}ms"

    /// Concurrent viewers each read the first page and some continue for two more pages.
    let readBurst name requester poolId matchId =
        async {
            let latencies = List<float>()
            let failures = ref 0
            let gate = obj ()

            let viewer index =
                async {
                    let rec read cursor pagesLeft =
                        async {
                            let watch = Stopwatch.StartNew()
                            let! outcome = ranked.ExecuteAsync(requester, poolId, matchId, cursor) |> Async.Catch
                            let elapsed = watch.Elapsed.TotalMilliseconds

                            match outcome with
                            | Choice1Of2(Ok page) ->
                                lock gate (fun () -> latencies.Add elapsed)

                                if pagesLeft > 0 && page.Next.IsSome then
                                    return! read page.Next (pagesLeft - 1)
                            | _ -> Interlocked.Increment(&failures.contents) |> ignore
                        }

                    return! read None (if index % 5 = 0 then 2 else 0)
                }

            let watch = Stopwatch.StartNew()
            do! Async.Parallel([ for index in 1..viewers -> viewer index ], 1000) |> Async.Ignore
            summarize name (List.ofSeq latencies) failures.Value
            measure $"{name} wall time" $"{watch.Elapsed.TotalSeconds:F1}s"
        }

    let operationDurations since =
        workers.Deliveries
        |> List.skip since
        |> List.filter (fun d -> d.Queue = "commands")
        |> List.map (fun d ->
            let outcome = if d.Succeeded then "ok" else "yield"
            $"{d.Elapsed.TotalSeconds:F1}s(rc={d.ReceiveCount},{outcome})")
        |> String.concat " "

    /// The authoritative first page: totals descending, gambler ID ascending.
    let authoritativeTop poolId count =
        async {
            let rec all cursor acc =
                async {
                    let! page, next = settlement.GetMembershipPageAsync(poolId, cursor, 1000)
                    let acc = acc @ page

                    match next with
                    | Some _ -> return! all next acc
                    | None -> return acc
                }

            let! everyone = all None []
            return PositionPolicy.rank everyone |> List.truncate count |> List.map (fun (m, _) -> m.GamblerId)
        }

    let awaitProjection requester poolId matchId name =
        async {
            let! expected = authoritativeTop poolId 50
            let watch = Stopwatch.StartNew()

            let rec poll () =
                async {
                    match! ranked.ExecuteAsync(requester, poolId, matchId, None) with
                    | Ok page when (page.Items |> Seq.map _.GamblerId |> List.ofSeq) = expected ->
                        measure $"{name} first page matches authoritative standings after" $"{watch.Elapsed.TotalSeconds:F1}s"
                    | _ when watch.Elapsed > TimeSpan.FromMinutes 30.0 -> measure $"{name} convergence" "NOT CONVERGED within 30 min"
                    | _ ->
                        do! Async.Sleep 2000
                        return! poll ()
                }

            return! poll ()
        }

    let runPipeline name commandWorkers maintenanceWorkers =
        async {
            let watch = Stopwatch.StartNew()
            let! elapsed = pipeline.RunUntilQuiescentAsync(TimeSpan.FromHours 2.0, commandWorkers, maintenanceWorkers)
            measure $"{name} pipeline idle after" $"{elapsed.TotalSeconds:F1}s"
            return watch.Elapsed
        }

    member _.Measurements = List.ofSeq measurements

    member this.RunAsync() =
        async {
            let poolId = Ulid.random()
            let otherPoolId = Ulid.random()
            let layoutId = Ulid.random().Value
            let memberIds = List.init members (fun _ -> Ulid.random().Value) |> List.sort
            let otherIds = List.init 50 (fun _ -> Ulid.random().Value) |> List.sort
            let now = DateTime.UtcNow

            let schedule =
                List.init matches (fun index ->
                    { MatchId = Ulid.random().Value
                      Kickoff = (if index < 4 then now.AddHours(-3.0 + float index * 0.1) else now.AddDays(float index))
                      Result = None })

            let generation (gamblerId: string) = $"G-{gamblerId}"

            // Every member has a prediction on the first matches so awards differ.
            let prediction index (matchIndex: int) =
                if index % 7 = 0 then None else Some((index + matchIndex) % 4, (index / 3 + matchIndex) % 3)

            let poolItems (pool: Ulid) (ids: string list) =
                seq {
                    yield rootItem pool.Value layoutId ids.Head ids.Length true

                    for index, gamblerId in List.indexed ids do
                        yield membershipItem pool.Value layoutId { GamblerId = gamblerId; Total = 0; Position = None } (Some(generation gamblerId))

                        for matchIndex, scheduled in List.indexed schedule do
                            yield
                                detailItem
                                    pool.Value
                                    layoutId
                                    gamblerId
                                    scheduled
                                    (if matchIndex < 4 then prediction index matchIndex else None)
                                    None
                                    (Some(generation gamblerId, 0))
                }

            let seedWatch = Stopwatch.StartNew()
            do! writeAll dynamo (VerificationEnvironment.poolLayoutTable ()) (schedule |> List.map (layoutMatchItem layoutId)) 4
            do! writeAll dynamo (VerificationEnvironment.poolTable ()) (poolItems poolId memberIds) 24
            do! writeAll dynamo (VerificationEnvironment.poolTable ()) (poolItems otherPoolId otherIds) 8
            measure "seeded items" $"{(members + 50) * (matches + 1) + 2} in {seedWatch.Elapsed.TotalSeconds:F0}s, throttled batch requests {seedingThrottles}"

            // The pool readiness a verified backfill would record.
            let! _ = projection.SetReadinessAsync(poolId, { Readiness = Ready; CheckedMembers = members; CheckedDetails = members * matches; Unresolved = 0 }, int64 members)
            let! _ = projection.SetReadinessAsync(otherPoolId, { Readiness = Ready; CheckedMembers = 50; CheckedDetails = 50 * matches; Unresolved = 0 }, 50L)

            // Start the stream pumps after seeding.
            let! _ = pipeline.PumpStreamsOnceAsync()
            let requester = Ulid.newOf memberIds.Head
            let match1 = Ulid.newOf schedule[0].MatchId
            let match2 = Ulid.newOf schedule[1].MatchId
            let match3 = Ulid.newOf schedule[2].MatchId

            do! readBurst "ranked reads, all-zero tie, idle" requester poolId match1

            // Result for match 1 in both pools; positions finalize inside each pool operation.
            let since = workers.Deliveries.Length
            do! layoutRepository.SetMatchFinalScoreAsync(Ulid.newOf layoutId, match1, 2, 1)

            let settleOnly =
                async {
                    let rec loop () =
                        async {
                            let! _ = pipeline.PumpStreamsOnceAsync()
                            let! _ = Async.Parallel([ workers.PollCommandsOnceAsync 1; workers.PollCommandsOnceAsync 1 ])
                            let! root = settlement.GetRootAsync poolId
                            let! receipt = settlement.GetReceiptAsync(poolId, $"RESULT#{match1.Value}#1")

                            match receipt with
                            | Some { State = Completed } when root.Value.ActiveOperation.IsNone -> ()
                            | _ -> return! loop ()
                        }

                    let watch = Stopwatch.StartNew()
                    do! loop ()
                    measure "match 1 settlement + positions (5,000-member pool)" $"{watch.Elapsed.TotalSeconds:F1}s; deliveries: {operationDurations since}"
                }

            do! settleOnly

            // Projection propagation alongside a viewer burst.
            let propagation = runPipeline "match 1 propagation (16 local maintenance workers)" 2 16 |> Async.StartChild
            let! running = propagation
            do! readBurst "ranked reads during propagation" requester poolId match1
            let! _ = running
            do! awaitProjection requester poolId match1 "match 1"

            // Consecutive results in the same pool, then a correction wave and a replay.
            let since = workers.Deliveries.Length
            do! layoutRepository.SetMatchFinalScoreAsync(Ulid.newOf layoutId, match2, 0, 0)
            do! layoutRepository.SetMatchFinalScoreAsync(Ulid.newOf layoutId, match3, 1, 1)
            let! elapsed = runPipeline "matches 2 and 3 back to back" 2 16
            measure "matches 2 and 3 operations" (operationDurations since)

            let since = workers.Deliveries.Length
            let! _ = layoutRepository.CorrectMatchResultAsync(Ulid.newOf layoutId, match1, 1, 1, 1)
            let! _ = runPipeline "match 1 correction wave" 2 16
            measure "correction operation" (operationDurations since)
            do! awaitProjection requester poolId match1 "after correction"

            let since = workers.Deliveries.Length
            do! (PoolCommandSqsPublisher(sqs, VerificationEnvironment.commandQueueUrl ()) :> IPoolCommandPublisher).RepublishAsync [ PoolCommand.settleResult poolId match1 { HomeTeamScore = 2; AwayTeamScore = 1; Version = 1 } ]
            let! _ = runPipeline "replay of a completed operation" 2 2
            measure "replay delivery" (operationDurations since)

            // Checkpoint yields: invocations with about 3 seconds of work each.
            let since = workers.Deliveries.Length
            workers.CommandRemainingTime <- TimeSpan.FromSeconds 48.0
            do! layoutRepository.SetMatchFinalScoreAsync(Ulid.newOf layoutId, Ulid.newOf schedule[3].MatchId, 3, 0)
            let! _ = runPipeline "match 4 with 3-second invocations" 2 16
            workers.CommandRemainingTime <- TimeSpan.FromMinutes 5.0
            let deliveries = workers.Deliveries |> List.skip since |> List.filter (fun d -> d.Queue = "commands")
            measure "match 4 yields" $"{deliveries |> List.filter (fun d -> not d.Succeeded) |> List.length} yields, max receive count {deliveries |> List.map _.ReceiveCount |> List.fold max 0}"

            do! readBurst "ranked reads after convergence" requester poolId match1

            let maintenance = workers.Deliveries |> List.filter (fun d -> d.Queue = "maintenance")
            measure "maintenance deliveries" $"{maintenance.Length}, failed {maintenance |> List.filter (fun d -> not d.Succeeded) |> List.length}"
            ignore elapsed
        }

    /// Single-viewer and moderate-concurrency page latency on the seeded pool, and the cost
    /// of the authoritative reads a position pass performs.
    member this.RunLatencyAsync() =
        async {
            let rec findRoot (start: Dictionary<string, Amazon.DynamoDBv2.Model.AttributeValue>) =
                async {
                    let! page =
                        dynamo.ScanAsync(
                            Amazon.DynamoDBv2.Model.ScanRequest(
                                TableName = VerificationEnvironment.poolTable (),
                                FilterExpression = "gamblerCount = :count AND pk = sk",
                                ExpressionAttributeValues = Dictionary(dict [ ":count", n members ]),
                                ExclusiveStartKey = start
                            )
                        )
                        |> Async.AwaitTask

                    match page.Items |> Seq.tryHead, page.LastEvaluatedKey |> Option.ofObj with
                    | Some item, _ -> return item
                    | None, Some next when next.Count > 0 -> return! findRoot next
                    | None, _ -> return failwith "No seeded load pool found"
                }

            let! root = findRoot null
            let poolId = Ulid.newOf root["poolId"].S
            let layoutId = root["poolLayoutId"].S

            let! layoutMatches =
                dynamo.QueryAsync(
                    Amazon.DynamoDBv2.Model.QueryRequest(
                        TableName = VerificationEnvironment.poolLayoutTable (),
                        KeyConditionExpression = "pk = :pk",
                        ExpressionAttributeValues = Dictionary(dict [ ":pk", s $"POOLLAYOUT#{layoutId}" ])
                    )
                )
                |> Async.AwaitTask

            let matchId = layoutMatches.Items |> Seq.find (fun item -> item.ContainsKey "homeTeamScore") |> fun item -> Ulid.newOf item["matchId"].S
            let! firstPage, _ = settlement.GetMembershipPageAsync(poolId, None, 1)
            let requester = firstPage.Head.GamblerId

            let timed () =
                async {
                    let watch = Stopwatch.StartNew()
                    let! result = ranked.ExecuteAsync(requester, poolId, matchId, None)
                    return (match result with Ok _ -> Some watch.Elapsed.TotalMilliseconds | Error _ -> None)
                }

            let! sequential = [ for _ in 1..200 -> timed () ] |> Async.Sequential
            let sequential = sequential |> Array.choose id |> List.ofArray
            summarize "single viewer, sequential first pages" sequential (200 - sequential.Length)

            for concurrency in [ 10; 50; 200 ] do
                let! samples = Async.Parallel([ for _ in 1..(concurrency * 5) -> timed () ], concurrency)
                let ok = samples |> Array.choose id |> List.ofArray
                summarize $"{concurrency} concurrent viewers" ok (samples.Length - ok.Length)

            // The authoritative reads of one position pass: every membership strongly, then sort.
            let readWatch = Stopwatch.StartNew()

            let rec all cursor acc pages =
                async {
                    let! page, next = settlement.GetMembershipPageAsync(poolId, cursor, 100)

                    match next with
                    | Some _ -> return! all next (acc @ page) (pages + 1)
                    | None -> return acc @ page, pages + 1
                }

            let! everyone, pages = all None [] 0
            let readTime = readWatch.Elapsed
            let sortWatch = Stopwatch.StartNew()
            let ranking = PositionPolicy.rank everyone
            measure "position pass membership reads" $"{everyone.Length} memberships in {pages} strongly consistent pages, {readTime.TotalSeconds:F2}s"
            measure "position pass sort" $"{ranking.Length} ranked in {sortWatch.Elapsed.TotalMilliseconds:F1}ms"

            let detailWatch = Stopwatch.StartNew()
            let! details = settlement.GetBetSettlementStatesAsync(poolId, matchId, everyone |> List.map _.GamblerId)
            measure "match detail strong batch reads" $"{details.Length} details in {detailWatch.Elapsed.TotalSeconds:F2}s"
        }
