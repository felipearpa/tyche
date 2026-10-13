namespace Felipearpa.Tyche.Verification

#nowarn "3536"

open System
open System.Collections.Generic
open System.Diagnostics
open Amazon.DynamoDBStreams
open Amazon.DynamoDBv2
open Amazon.DynamoDBv2.Model
open Amazon.Lambda.DynamoDBEvents
open Amazon.Lambda.TestUtilities
open Felipearpa.Core.Paging
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.AmazonLambda.Event
open Felipearpa.Tyche.Pool.Application
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Infrastructure
open Felipearpa.Tyche.Pool.Type
open Felipearpa.Tyche.PoolLayout.Application
open Felipearpa.Tyche.PoolLayout.Infrastructure
open Felipearpa.Type
open Felipearpa.Tyche.Verification.Seed

/// A score row as the leaderboard and My pools return it, reduced to what the sequence checks.
type StandingSummary =
    { Gambler: string
      Score: int option
      Position: int option
      BeforePosition: int option
      /// The trend group's kickoff label, computed matches, and total matches.
      TrendGroup: (string * int * int) option }

/// A kickoff group of History or Timeline, reduced to what the sequence checks.
type GroupItemSummary =
    { Kickoff: string
      Computed: int
      Total: int
      Matches: string list
      Movement: (int * int) option }

/// The kickoff-group settlement sequence (task 5.1 of group-kickoff-rank-movement) on isolated
/// tables, streams, and queues. Two gamblers, A and T, share a pool whose layout has M_old at
/// K1, M_a and M_b at K2, and M_late at K3. Results arrive for M_a, then M_b, then the older
/// M_old, and M_late is postponed by a day. After each step the leaderboard, My pools, History,
/// Timeline, the pool root, and the per-match snapshots are read through the production
/// services and repositories the endpoints use.
type KickoffGroupSequence
    (
        dynamo: IAmazonDynamoDB,
        pipeline: Pipeline,
        streams: AmazonDynamoDBStreamsClient,
        layoutStreamArn: string
    ) =

    let checks = List<Check>()

    let show value = Text.RegularExpressions.Regex.Replace(sprintf "%A" value, @"\s+", " ")

    let check name passed detail =
        checks.Add { Name = name; Passed = passed; Detail = detail }
        printfn "%s %s — %s" (if passed then "PASS" else "FAIL") name detail

    let keySerializer = DynamoDbKeySerializer()
    let settlement = PoolSettlementDynamoDbRepository(dynamo) :> IPoolSettlementRepository
    let layoutRepository = PoolLayoutDynamoDbRepository(keySerializer, dynamo) :> Felipearpa.Tyche.PoolLayout.Domain.IPoolLayoutRepository
    let pools = PoolDynamoDbRepository(keySerializer, dynamo) :> IPoolRepository
    let bets = PoolGamblerBetDynamoDbRepository(keySerializer, dynamo) :> IPoolGamblerBetRepository
    let scores = PoolGamblerScoreDynamoDbRepository(keySerializer, dynamo) :> IPoolGamblerScoreRepository
    let kickoffGroups = KickoffGroupReader(PoolLayoutMatchReader(layoutRepository))
    let standings = PoolStandings(scores, kickoffGroups)
    let leaderboard = GetPoolGamblerScoresByPool(scores, standings)
    let myPools = GetPoolGamblerScoresByGambler(scores, standings)
    let history = GetFinishedPoolGamblerBets(bets, kickoffGroups)
    let timeline = GetGamblerBetsTimeline(bets, kickoffGroups)

    let ids count = List.init count (fun _ -> Ulid.random().Value) |> List.sort

    let settle () = pipeline.RunUntilQuiescentAsync(TimeSpan.FromMinutes 10.0)

    /// Polls a read until it is accepted or a minute passes; index reads are eventually
    /// consistent.
    let await (read: unit -> Async<'T>) (accept: 'T -> bool) =
        async {
            let watch = Stopwatch.StartNew()

            let rec poll () =
                async {
                    let! value = read ()

                    if accept value then
                        return true, value, watch.Elapsed
                    elif watch.Elapsed > TimeSpan.FromSeconds 60.0 then
                        return false, value, watch.Elapsed
                    else
                        do! Async.Sleep 500
                        return! poll ()
                }

            return! poll ()
        }

    let detailKey (poolId: string) (gamblerId: string) (matchId: string) =
        Dictionary(dict [ "pk", s $"GAMBLER#{gamblerId}#POOL#{poolId}"; "sk", s $"MATCH#{matchId}" ])

    let readDetail poolId gamblerId matchId =
        async {
            let! response =
                dynamo.GetItemAsync(
                    GetItemRequest(TableName = PoolTable.name, Key = detailKey poolId gamblerId matchId, ConsistentRead = true)
                )
                |> Async.AwaitTask

            return response.Item
        }

    let intAttribute (name: string) (item: Dictionary<string, AttributeValue>) =
        match item.TryGetValue name with
        | true, value when not (isNull value.N) -> Some(int value.N)
        | _ -> None

    let itemText (item: Dictionary<string, AttributeValue>) =
        item
        |> Seq.sortBy _.Key
        |> Seq.map (fun pair ->
            let value = pair.Value
            let text = if not (isNull value.S) then value.S elif not (isNull value.N) then value.N else show value.BOOL
            $"{pair.Key}={text}")
        |> String.concat ";"

    /// Writes the gambler's prediction with the attributes `BetAsync` sets. The fixture's
    /// kickoffs are in the past, so that Timeline reveals the rows; `BetAsync` would refuse them
    /// with its lock condition, which is the only part left out.
    let predict poolId gamblerId matchId (home: int, away: int) =
        dynamo.UpdateItemAsync(
            UpdateItemRequest(
                TableName = PoolTable.name,
                Key = detailKey poolId gamblerId matchId,
                UpdateExpression = "SET #home = :home, #away = :away",
                ConditionExpression = "attribute_exists(#pk)",
                ExpressionAttributeNames = Dictionary(dict [ "#home", "homeTeamBet"; "#away", "awayTeamBet"; "#pk", "pk" ]),
                ExpressionAttributeValues = Dictionary(dict [ ":home", n home; ":away", n away ])
            )
        )
        |> Async.AwaitTask
        |> Async.Ignore

    let rec collectPages (read: string option -> Async<KickoffGroupItem CursorPage>) (next: string option) (pages: (string option * KickoffGroupItem list) list) =
        async {
            if pages.Length >= 20 then
                failwith "the grouped traversal did not end"

            let! page = read next
            let pages = pages @ [ next, List.ofSeq page.Items ]

            match page.Next with
            | Some _ -> return! collectPages read page.Next pages
            | None -> return pages
        }

    member _.Checks = List.ofSeq checks

    member this.RunAsync() =
        async {
            let poolId = Ulid.random().Value
            let layoutId = Ulid.random().Value
            let pool = Ulid.newOf poolId

            match ids 2, ids 4 with
            | [ a; t ], [ mOld; mA; mB; mLate ] ->
                let day = DateTime.UtcNow.Date
                let k1 = day.AddDays(-6.0).AddHours 18.0
                let k2 = day.AddDays(-5.0).AddHours 20.0
                let k3 = day.AddDays(-4.0).AddHours 20.0
                let k3Moved = k3.AddDays 1.0

                let kickoffLabel (kickoff: DateTime) =
                    let instant = KickoffGroup.instant kickoff

                    [ k1, "K1"; k2, "K2"; k3, "K3"; k3Moved, "K3+1d" ]
                    |> List.tryFind (fun (candidate, _) -> candidate = instant)
                    |> Option.map snd
                    |> Option.defaultValue (instant.ToString "o")

                let matchLabel (matchId: string) =
                    [ mOld, "M_old"; mA, "M_a"; mB, "M_b"; mLate, "M_late" ] |> List.find (fst >> (=) matchId) |> snd

                let gamblerLabel (gamblerId: string) = if gamblerId = a then "A" else "T"

                let seedMatch matchId kickoff = { MatchId = matchId; Kickoff = kickoff; Result = None }

                let predictions =
                    [ mOld, (1, 0), (0, 2)
                      mA, (3, 0), (2, 1)
                      mB, (1, 1), (2, 0)
                      mLate, (1, 0), (0, 0) ]

                // The layout, then the stream pumps start reading after it.
                do!
                    writeAll
                        dynamo
                        PoolLayoutTable.name
                        [ layoutMatchItem layoutId (seedMatch mOld k1)
                          layoutMatchItem layoutId (seedMatch mA k2)
                          layoutMatchItem layoutId (seedMatch mB k2)
                          layoutMatchItem layoutId (seedMatch mLate k3) ]
                        1

                let! _ = settle ()

                // A creates the pool and T joins, as the endpoints do; the stream handlers
                // materialize both bet sheets and place both members.
                let! created =
                    pools.CreatePoolAsync
                        { PoolId = pool
                          PoolName = NonEmptyString100.newOf "Verification 5.1"
                          PoolLayoutId = Ulid.newOf layoutId
                          PoolLayoutVersion = 1
                          OwnerGamblerId = Ulid.newOf a
                          OwnerGamblerUsername = NonEmptyString100.newOf "gambler-a"
                          OwnerGamblerEmail = NonEmptyString100.newOf "a@verification.invalid" }

                let! joined =
                    pools.JoinPoolAsync
                        { PoolId = pool
                          PoolName = NonEmptyString100.newOf "Verification 5.1"
                          GamblerId = Ulid.newOf t
                          GamblerUsername = NonEmptyString100.newOf "gambler-t"
                          GamblerEmail = NonEmptyString100.newOf "t@verification.invalid"
                          PoolLayoutId = Ulid.newOf layoutId
                          PoolLayoutVersion = 1 }

                check "pool created and joined" (Result.isOk created && Result.isOk joined) (show (created, joined))
                let! _ = settle ()

                for matchId, predictionA, predictionT in predictions do
                    do! predict poolId a matchId predictionA
                    do! predict poolId t matchId predictionT

                let! _ = settle ()

                let summarizeStanding (standing: PoolStanding) =
                    { Gambler = gamblerLabel standing.Score.GamblerId.Value
                      Score = standing.Score.Score
                      Position = standing.Score.Position
                      BeforePosition = standing.Score.BeforePosition
                      TrendGroup =
                        standing.TrendGroup
                        |> Option.map (fun group -> kickoffLabel group.KickoffDateTime, group.ComputedMatches, group.TotalMatches) }

                let readLeaderboard () =
                    async {
                        let! page = leaderboard.ExecuteAsync(pool, None)
                        return page.Items |> Seq.map summarizeStanding |> List.ofSeq
                    }

                let readMyPools () =
                    async {
                        let! rowsA = myPools.ExecuteAsync(Ulid.newOf a, None)
                        let! rowsT = myPools.ExecuteAsync(Ulid.newOf t, None)

                        return
                            Seq.append rowsA.Items rowsT.Items
                            |> Seq.filter (fun standing -> standing.Score.PoolId = pool)
                            |> Seq.map summarizeStanding
                            |> List.ofSeq
                    }

                let summarizeGroup (item: KickoffGroupItem) =
                    { Kickoff = kickoffLabel item.Group.KickoffDateTime
                      Computed = item.Group.ComputedMatches
                      Total = item.Group.TotalMatches
                      Matches = item.Bets |> List.map (fun bet -> matchLabel bet.MatchId.Value)
                      Movement = item.Movement |> Option.map (fun movement -> movement.BeforePosition, movement.Position) }

                let readHistory gamblerId () =
                    async {
                        let! pages = collectPages (fun next -> history.ExecuteAsync(pool, Ulid.newOf gamblerId, next)) None []
                        return pages |> List.collect (snd >> List.map summarizeGroup)
                    }

                // T's Timeline as A requests it; the endpoint's membership check is not part of
                // this read.
                let readTimeline () =
                    async {
                        let! pages = collectPages (fun next -> timeline.ExecuteAsync(pool, Ulid.newOf t, next)) None []
                        return pages |> List.collect (snd >> List.map summarizeGroup)
                    }

                let readTrendKickoff () =
                    async {
                        let! root = settlement.GetRootAsync pool
                        return root |> Option.bind _.TrendKickoffDateTime |> Option.map kickoffLabel
                    }

                let readSnapshot matchId =
                    async {
                        let! itemA = readDetail poolId a matchId
                        let! itemT = readDetail poolId t matchId

                        return
                            (intAttribute "beforePosition" itemA, intAttribute "position" itemA),
                            (intAttribute "beforePosition" itemT, intAttribute "position" itemT)
                    }

                let expectRead step name read (accept: 'T -> bool) =
                    async {
                        let! passed, value, elapsed = await read accept
                        check $"{step}: {name}" passed $"{show value} (read after {elapsed.TotalSeconds:F1} s)"
                    }

                let expectEqual step name read (expected: 'T) = expectRead step name read ((=) expected)

                let row gambler score position before trend =
                    { Gambler = gambler
                      Score = Some score
                      Position = Some position
                      BeforePosition = Some before
                      TrendGroup = trend }

                let group kickoff computed total matches movement =
                    { Kickoff = kickoff
                      Computed = computed
                      Total = total
                      Matches = matches
                      Movement = movement }

                let byGambler = List.sortBy (fun (summary: StandingSummary) -> summary.Gambler)

                let expectStandings step (rows: StandingSummary list) =
                    async {
                        let ordered = rows |> List.sortBy _.Position
                        do! expectEqual step "leaderboard rows with trendGroup" readLeaderboard ordered
                        do! expectRead step "My pools rows with trendGroup" readMyPools (fun found -> byGambler found = byGambler rows)
                    }

                let settleResult matchId (home, away) =
                    async {
                        do! layoutRepository.SetMatchFinalScoreAsync(Ulid.newOf layoutId, Ulid.newOf matchId, home, away)
                        let! _ = settle ()
                        return ()
                    }

                // Step 0: no settled match.
                let step = "step 0 (no result)"

                do!
                    expectRead
                        step
                        "positions A 1, T 2 and no trendGroup"
                        readLeaderboard
                        (fun rows ->
                            rows |> List.map (fun row -> row.Gambler, row.Position, row.TrendGroup) = [ "A", Some 1, None; "T", Some 2, None ])

                do!
                    expectRead
                        step
                        "My pools rows without trendGroup"
                        readMyPools
                        (fun rows -> rows.Length = 2 && rows |> List.forall (fun row -> row.TrendGroup.IsNone))

                do! expectEqual step "root trend kickoff" readTrendKickoff None
                do! expectEqual step "A's History" (readHistory a) []

                do!
                    expectEqual
                        step
                        "T's Timeline"
                        readTimeline
                        [ group "K1" 0 1 [ "M_old" ] None
                          group "K2" 0 2 [ "M_a"; "M_b" ] None
                          group "K3" 0 1 [ "M_late" ] None ]

                // Step 1: M_a (2-1) opens K2.
                do! settleResult mA (2, 1)
                let step = "step 1 (M_a)"
                let k2Partial = Some("K2", 1, 2)
                do! expectStandings step [ row "A" 10 2 1 k2Partial; row "T" 20 1 2 k2Partial ]
                do! expectEqual step "root trend kickoff" readTrendKickoff (Some "K2")
                do! expectEqual step "A's History" (readHistory a) [ group "K2" 1 2 [ "M_a" ] (Some(1, 2)) ]
                do! expectEqual step "T's History" (readHistory t) [ group "K2" 1 2 [ "M_a" ] (Some(2, 1)) ]

                do!
                    expectEqual
                        step
                        "T's Timeline: K2 partial in the unsettled phase"
                        readTimeline
                        [ group "K1" 0 1 [ "M_old" ] None
                          group "K2" 1 2 [ "M_a"; "M_b" ] (Some(2, 1))
                          group "K3" 0 1 [ "M_late" ] None ]

                do! expectEqual step "M_a snapshots (A, T)" (fun () -> readSnapshot mA) ((Some 1, Some 2), (Some 2, Some 1))

                // Step 2: M_b (1-1) completes K2 from the same baseline.
                do! settleResult mB (1, 1)
                let step = "step 2 (M_b)"
                let k2Complete = Some("K2", 2, 2)
                do! expectStandings step [ row "A" 30 1 1 k2Complete; row "T" 20 2 2 k2Complete ]
                do! expectEqual step "root trend kickoff" readTrendKickoff (Some "K2")
                do! expectEqual step "A's History" (readHistory a) [ group "K2" 2 2 [ "M_a"; "M_b" ] (Some(1, 1)) ]
                do! expectEqual step "T's History" (readHistory t) [ group "K2" 2 2 [ "M_a"; "M_b" ] (Some(2, 2)) ]

                do!
                    expectEqual
                        step
                        "T's Timeline: K2 once, in the finished phase"
                        readTimeline
                        [ group "K1" 0 1 [ "M_old" ] None
                          group "K3" 0 1 [ "M_late" ] None
                          group "K2" 2 2 [ "M_a"; "M_b" ] (Some(2, 2)) ]

                do! expectEqual step "M_b snapshots (A, T)" (fun () -> readSnapshot mB) ((Some 2, Some 1), (Some 1, Some 2))

                // Step 3: M_old (0-2) is a late result for the older K1.
                do! settleResult mOld (0, 2)
                let step = "step 3 (M_old, late)"
                do! expectStandings step [ row "A" 30 2 1 k2Complete; row "T" 40 1 2 k2Complete ]
                do! expectEqual step "root trend kickoff stays K2" readTrendKickoff (Some "K2")

                let historyA =
                    [ group "K2" 2 2 [ "M_a"; "M_b" ] (Some(1, 1))
                      group "K1" 1 1 [ "M_old" ] (Some(1, 2)) ]

                let historyT =
                    [ group "K2" 2 2 [ "M_a"; "M_b" ] (Some(2, 2))
                      group "K1" 1 1 [ "M_old" ] (Some(2, 1)) ]

                do! expectEqual step "A's History" (readHistory a) historyA
                do! expectEqual step "T's History" (readHistory t) historyT

                let timelineAfterResults moved =
                    [ group moved 0 1 [ "M_late" ] None
                      group "K2" 2 2 [ "M_a"; "M_b" ] (Some(2, 2))
                      group "K1" 1 1 [ "M_old" ] (Some(2, 1)) ]

                do! expectEqual step "T's Timeline" readTimeline (timelineAfterResults "K3")
                do! expectEqual step "M_old snapshots (A, T)" (fun () -> readSnapshot mOld) ((Some 1, Some 2), (Some 2, Some 1))
                do! expectEqual step "M_a snapshots unchanged" (fun () -> readSnapshot mA) ((Some 1, Some 2), (Some 2, Some 1))
                do! expectEqual step "M_b snapshots unchanged" (fun () -> readSnapshot mB) ((Some 2, Some 1), (Some 1, Some 2))

                // Step 4: M_late is postponed by a day, as the operator procedure does it.
                do!
                    dynamo.UpdateItemAsync(
                        UpdateItemRequest(
                            TableName = PoolLayoutTable.name,
                            Key = Dictionary(dict [ "pk", s $"POOLLAYOUT#{layoutId}"; "sk", s $"MATCH#{mLate}" ]),
                            UpdateExpression = "SET #kickoff = :kickoff",
                            ExpressionAttributeNames = Dictionary(dict [ "#kickoff", "matchDateTime" ]),
                            ExpressionAttributeValues = Dictionary(dict [ ":kickoff", s (instant k3Moved) ])
                        )
                    )
                    |> Async.AwaitTask
                    |> Async.Ignore

                let! _ = settle ()
                let step = "step 4 (M_late postponed)"

                let readLateKickoffs () =
                    async {
                        let! itemA = readDetail poolId a mLate
                        let! itemT = readDetail poolId t mLate
                        return [ itemA; itemT ] |> List.map (fun item -> kickoffLabel (DateTime.Parse(item["matchDateTime"].S).ToUniversalTime()))
                    }

                do! expectEqual step "every M_late detail moved to the new kickoff" readLateKickoffs [ "K3+1d"; "K3+1d" ]
                do! expectStandings step [ row "A" 30 2 1 k2Complete; row "T" 40 1 2 k2Complete ]
                do! expectEqual step "root trend kickoff stays K2" readTrendKickoff (Some "K2")
                do! expectEqual step "T's Timeline: M_late under the new kickoff only" readTimeline (timelineAfterResults "K3+1d")
                do! expectEqual step "A's History unchanged" (readHistory a) historyA

                // The same stream record delivered again writes nothing.
                let redeliveryPump = StreamPump(streams, layoutStreamArn, false)

                let isPostponement (record: DynamoDBEvent.DynamodbStreamRecord) =
                    record.EventName = "MODIFY"
                    && record.Dynamodb.Keys["sk"].S = $"MATCH#{mLate}"
                    && record.Dynamodb.OldImage["matchDateTime"].S <> record.Dynamodb.NewImage["matchDateTime"].S

                let rec findPostponement attempts =
                    async {
                        let! records = redeliveryPump.PollAsync()

                        match records |> List.tryFind isPostponement with
                        | Some record -> return Some record
                        | None when attempts < 20 ->
                            do! Async.Sleep 500
                            return! findPostponement (attempts + 1)
                        | None -> return None
                    }

                match! findPostponement 0 with
                | None -> check $"{step}: second delivery writes nothing" false "the postponement record was not found on the layout stream"
                | Some record ->
                    let readLateDetails () =
                        async {
                            let! itemA = readDetail poolId a mLate
                            let! itemT = readDetail poolId t mLate
                            return itemText itemA, itemText itemT
                        }

                    let! before = readLateDetails ()

                    let! response =
                        PoolLayoutEvent().OnPoolLayoutChangeAsync(DynamoDBEvent(Records = List([ record ])), TestLambdaContext())
                        |> Async.AwaitTask

                    do! Async.Sleep 5000
                    let! first = pipeline.PumpStreamsOnceAsync()
                    do! Async.Sleep 2000
                    let! second = pipeline.PumpStreamsOnceAsync()
                    let! after = readLateDetails ()

                    check
                        $"{step}: second delivery writes nothing"
                        (response.BatchItemFailures.Count = 0 && first + second = 0 && before = after)
                        $"redelivered stream record {record.EventID}: {response.BatchItemFailures.Count} batch failures, {first + second} new stream records afterwards, M_late details unchanged: {before = after}"

                check "stream batches recovered" true $"retried stream deliveries: {pipeline.StreamFailures}"
            | _ -> ()
        }
