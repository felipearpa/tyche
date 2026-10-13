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

/// The rank baseline resets once per kickoff group: the first settlement of a newer kickoff
/// records it, later settlements of the same group, late results, and corrections keep it, and
/// completion records the group's kickoff on the pool root.
module KickoffGroupSettlementTest =

    let private pool = Ulid.newOf poolId

    /// A match older than the default group that gets its result only after newer groups settled.
    let private lateMatch = "01K1PX1TX2NM1HG851S1V0M004"

    let private groupKickoff = pastKickoff
    let private newerKickoff = pastKickoff.AddHours 2.0

    /// match1 and match2 share one kickoff, match3 is newer, and the late match is older.
    let private matches =
        [ { MatchId = match1
            MatchDateTime = groupKickoff
            Result = None }
          { MatchId = match2
            MatchDateTime = groupKickoff
            Result = None }
          { MatchId = match3
            MatchDateTime = newerKickoff
            Result = None }
          { MatchId = lateMatch
            MatchDateTime = pastKickoff.AddDays -1.0
            Result = None } ]

    /// A 4 points (3rd), B 10 (1st), C 6 (2nd) before the group.
    let private members =
        [ { GamblerId = gamblerA
            Total = 4
            Position = Some 3
            Predictions = [ match0, (2, 5); match1, (1, 0); match2, (0, 0) ] }
          { GamblerId = gamblerB
            Total = 10
            Position = Some 1
            Predictions = [ match0, (3, 0); match1, (2, 2); match2, (1, 0); lateMatch, (5, 5) ] }
          { GamblerId = gamblerC
            Total = 6
            Position = Some 2
            Predictions = [ match0, (2, 3); match2, (3, 1) ] } ]

    let private gamblers = [ gamblerA; gamblerB; gamblerC ]

    let private seeded (options: HarnessOptions) =
        async {
            let harness = VersionedSettlementHarness(options)
            do! harness.SeedAndMigrateAsync(members, matches)
            return harness
        }

    let private positions (harness: VersionedSettlementHarness) = gamblers |> List.map (position harness.Fake)

    let private baselines (harness: VersionedSettlementHarness) = gamblers |> List.map (beforePosition harness.Fake)

    let private trendKickoff (harness: VersionedSettlementHarness) =
        root harness.Fake
        |> text "trendKickoffDateTime"
        |> Option.map (fun value -> DateTime.Parse(value, CultureInfo.InvariantCulture, DateTimeStyles.RoundtripKind))

    let private utc (value: DateTime) = value.ToUniversalTime()

    [<Fact>]
    let ``matches of one kickoff keep the baseline taken before the group`` () =
        async {
            let! harness = seeded HarnessOptions.Default
            trendKickoff harness |> shouldEqual None

            // A scores 20: A 24, B 10, C 6.
            do! harness.SettleAsync(match1, 1, 0)
            positions harness |> shouldEqual [ Some 1; Some 2; Some 3 ]
            baselines harness |> shouldEqual [ Some 3; Some 1; Some 2 ]
            trendKickoff harness |> shouldEqual (Some(utc groupKickoff))

            // B scores 10 and C 20: C 26, A 24, B 20. The baseline is still the one before the group.
            do! harness.SettleAsync(match2, 3, 1)
            positions harness |> shouldEqual [ Some 2; Some 3; Some 1 ]
            baselines harness |> shouldEqual [ Some 3; Some 1; Some 2 ]
            trendKickoff harness |> shouldEqual (Some(utc groupKickoff))

            // Each settled match detail still holds its own snapshot.
            gamblers |> List.map (fun g -> detailBeforePosition harness.Fake g match2) |> shouldEqual [ Some 1; Some 2; Some 3 ]
            gamblers |> List.map (fun g -> detailPosition harness.Fake g match2) |> shouldEqual [ Some 2; Some 3; Some 1 ]
        }

    [<Fact>]
    let ``a newer kickoff resets the baseline and older results and corrections keep it`` () =
        async {
            let! harness = seeded HarnessOptions.Default
            do! harness.SettleAsync(match1, 1, 0)
            do! harness.SettleAsync(match2, 3, 1)

            // Nobody predicted match 3: positions stay C, A, B and the baseline restarts there.
            do! harness.SettleAsync(match3, 0, 0)
            positions harness |> shouldEqual [ Some 2; Some 3; Some 1 ]
            baselines harness |> shouldEqual [ Some 2; Some 3; Some 1 ]
            trendKickoff harness |> shouldEqual (Some(utc newerKickoff))

            // A late result of an older kickoff: B scores 20 and leads, the baseline stays.
            do! harness.SettleAsync(lateMatch, 5, 5)
            positions harness |> shouldEqual [ Some 3; Some 1; Some 2 ]
            baselines harness |> shouldEqual [ Some 2; Some 3; Some 1 ]
            trendKickoff harness |> shouldEqual (Some(utc newerKickoff))
            detailBeforePosition harness.Fake gamblerB lateMatch |> shouldEqual (Some 3)

            // A correction of the older group: A loses 20 and B gains 20; the baseline stays.
            let! accepted = harness.CorrectResultAsync(match1, 2, 2)
            accepted |> shouldEqual true
            do! harness.RunUntilQuiescentAsync()
            [ total harness.Fake gamblerA; total harness.Fake gamblerB ] |> shouldEqual [ Some 4; Some 60 ]
            positions harness |> shouldEqual [ Some 3; Some 1; Some 2 ]
            baselines harness |> shouldEqual [ Some 2; Some 3; Some 1 ]
            trendKickoff harness |> shouldEqual (Some(utc newerKickoff))
        }

    [<Fact>]
    let ``operations resumed after deadline yields apply the same baseline decision`` () =
        async {
            let options =
                { HarnessOptions.Default with
                    Settings = { PoolOperationSettings.Default with MembershipPageSize = 1 }
                    Tick = TimeSpan.FromSeconds 1.0
                    InvocationBudget = TimeSpan.FromSeconds 6.0 }

            let! harness = seeded options
            do! harness.SettleAsync(match1, 1, 0)
            do! harness.SettleAsync(match2, 3, 1)

            harness.CommandOutcomes |> List.map _.Outcome |> List.contains PoolCommandOutcome.Yielded |> shouldEqual true
            harness.Commands.DeadLetters |> shouldEqual []
            positions harness |> shouldEqual [ Some 2; Some 3; Some 1 ]
            baselines harness |> shouldEqual [ Some 3; Some 1; Some 2 ]
            trendKickoff harness |> shouldEqual (Some(utc groupKickoff))
        }

    [<Fact>]
    let ``the decision is taken at admission and survives a later change of the root`` () =
        async {
            let! harness = seeded HarnessOptions.Default
            do! harness.SettleAsync(match1, 1, 0)
            harness.SetLayoutResult(match2, 3, 1)

            let command =
                PoolCommand.settleResult
                    pool
                    (Ulid.newOf match2)
                    { HomeTeamScore = 3
                      AwayTeamScore = 1
                      Version = 1 }

            // Admitted, then yielded at once: the checkpoint holds the decision.
            let! yielded = harness.ProcessCommand.ExecuteAsync(command, harness.Now())
            yielded.Outcome |> shouldEqual PoolCommandOutcome.Yielded
            let admitted = root harness.Fake |> Option.get
            admitted["activeOperation"].M["trendEffect"].S |> shouldEqual "KEEPS"

            // Even if the root lost its trend kickoff meanwhile, the resumed operation keeps the baseline.
            let rootItem = root harness.Fake |> Option.get
            rootItem.Remove "trendKickoffDateTime" |> ignore
            harness.Fake.Put("Pool", rootItem)

            let! resumed = harness.ProcessCommand.ExecuteAsync(command, harness.Now().AddMinutes 5.0)
            resumed.Outcome |> shouldEqual PoolCommandOutcome.OperationCompleted
            baselines harness |> shouldEqual [ Some 3; Some 1; Some 2 ]
            trendKickoff harness |> shouldEqual None
        }

    [<Fact>]
    let ``a computed detail records its kickoff once and keeps it on corrections`` () =
        async {
            let! harness = seeded HarnessOptions.Default
            do! harness.SettleAsync(match1, 1, 0)

            let kickoffOf gamblerId = detail harness.Fake gamblerId match1 |> text "computedMatchDateTime"
            let original = detail harness.Fake gamblerA match1 |> text "matchDateTime"
            original |> Option.isSome |> shouldEqual true
            gamblers |> List.map kickoffOf |> shouldEqual [ original; original; original ]
            detail harness.Fake gamblerA match2 |> has "computedMatchDateTime" |> shouldEqual false

            // A correction does not rewrite it, even if the detail's own kickoff copy differs.
            let item = detail harness.Fake gamblerA match1 |> Option.get
            item["matchDateTime"] <- AttributeValue(S = newerKickoff.ToUniversalTime().ToString("o"))
            harness.Fake.Put("Pool", item)
            let! _ = harness.CorrectResultAsync(match1, 2, 2)
            do! harness.RunUntilQuiescentAsync()

            kickoffOf gamblerA |> shouldEqual original
            award harness.Fake gamblerA match1 |> shouldEqual (Some 0)
        }

    [<Fact>]
    let ``the finished-by-kickoff index holds only computed details, newest kickoff first`` () =
        async {
            let! harness = seeded HarnessOptions.Default
            do! harness.SettleAsync(match1, 1, 0)
            do! harness.SettleAsync(match3, 0, 0)

            let query indexName =
                QueryRequest(
                    TableName = "Pool",
                    IndexName = indexName,
                    KeyConditionExpression = "#pk = :pk",
                    ExpressionAttributeNames = Dictionary(dict [ "#pk", "pk" ]),
                    ExpressionAttributeValues =
                        Dictionary(dict [ ":pk", AttributeValue(S = $"GAMBLER#{gamblerA}#POOL#{poolId}") ]),
                    ScanIndexForward = false
                )
                |> harness.Fake.Client.QueryAsync
                |> Async.AwaitTask

            let! byKickoff = query "GetFinishedPoolGamblerBetsByMatchDate-index"
            let! finished = query "GetFinishedPoolGamblerBets-index"

            // match0 was scored before this change and is not backfilled here; match2 and the
            // late match are pending.
            byKickoff.Items |> Seq.map (fun item -> item["matchId"].S) |> List.ofSeq |> shouldEqual [ match3; match1 ]

            // The existing finished index still holds every computed detail, by computation time.
            finished.Items
            |> Seq.map (fun item -> item["matchId"].S)
            |> Set.ofSeq
            |> shouldEqual (Set.ofList [ match0; match1; match3 ])
        }

    [<Fact>]
    let ``a checkpoint stores the trend decision, and one stored without it reads as ungrouped`` () =
        let checkpoint trend : ActiveOperation =
            { OperationId = $"RESULT#{match1}#1"
              Revision = 2L
              Target =
                MatchSettlement(
                    Ulid.newOf match1,
                    { HomeTeamScore = 1
                      AwayTeamScore = 0
                      Version = 1 },
                    trend
                )
              Stage = Positions
              Cursor = None
              CalculationRevision = 1
              MembershipRevision = 0L
              OwnerToken = "owner"
              LeaseExpiresAt = DateTime(2026, 6, 12, 18, 0, 0, DateTimeKind.Utc)
              AttemptsWithoutProgress = 0
              Blocked = false
              UnresolvedDetails = 0
              RestoredDetails = 0
              AdmittedAt = DateTime(2026, 6, 12, 17, 0, 0, DateTimeKind.Utc)
              LastProgressAt = DateTime(2026, 6, 12, 17, 0, 0, DateTimeKind.Utc) }

        let roundTrip trend =
            (checkpoint trend
             |> Felipearpa.Tyche.Pool.Infrastructure.PoolSettlementDictionaryTransformer.activeOperationAttribute)
                .M
            |> Felipearpa.Tyche.Pool.Infrastructure.PoolSettlementDictionaryTransformer.toActiveOperation
            |> _.Target

        let opening = OpensGroup(DateTime(2026, 6, 12, 17, 0, 0, DateTimeKind.Utc))
        roundTrip opening |> shouldEqual (checkpoint opening).Target
        roundTrip KeepsGroup |> shouldEqual (checkpoint KeepsGroup).Target
        roundTrip Ungrouped |> shouldEqual (checkpoint Ungrouped).Target
