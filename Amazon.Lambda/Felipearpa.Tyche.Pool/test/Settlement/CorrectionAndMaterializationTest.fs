namespace Felipearpa.Tyche.Pool.Test.Settlement

open System
open Amazon.DynamoDBv2.Model
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Test.Fakes
open Felipearpa.Tyche.Pool.Test.Settlement.PoolScenario
open Felipearpa.Type
open FsUnitTyped
open Xunit

/// Versioned result corrections, and materialization that races a result.
module CorrectionAndMaterializationTest =

    let private pool = Ulid.newOf poolId

    let private migrated () =
        async {
            let harness = VersionedSettlementHarness()
            do! harness.SeedAndMigrateAsync(defaultMembers, defaultMatches)
            return harness
        }

    let private touchesDetail gamblerId matchId (request: obj) =
        match request with
        | :? TransactWriteItemsRequest as transaction ->
            transaction.TransactItems
            |> Seq.exists (fun item ->
                let key =
                    if not (isNull item.Put) then item.Put.Item
                    elif not (isNull item.Update) then item.Update.Key
                    else null

                not (isNull key)
                && key["pk"].S = $"GAMBLER#{gamblerId}#POOL#{poolId}"
                && key["sk"].S = $"MATCH#{matchId}")
        | _ -> false

    [<Fact>]
    let ``a correction applies the award difference once and advances the score version`` () =
        async {
            let! harness = migrated ()
            do! harness.SettleAsync(match1, 1, 0)
            let computedAt = detail harness.Fake gamblerA match1 |> text "computedDateTime"
            let versionBefore = membership harness.Fake gamblerA |> number "scoreVersion"

            // 1-0 becomes 2-0: A's exact 1-0 (+20) becomes winner and away score (+14);
            // B's 2-2 (0) gains the home score (+4).
            let! accepted = harness.CorrectResultAsync(match1, 2, 0)
            do! harness.RunUntilQuiescentAsync()

            accepted |> shouldEqual true
            [ award harness.Fake gamblerA match1; award harness.Fake gamblerB match1 ] |> shouldEqual [ Some 14; Some 4 ]
            [ total harness.Fake gamblerA; total harness.Fake gamblerB; total harness.Fake gamblerC ] |> shouldEqual [ Some 18; Some 14; Some 6 ]
            detail harness.Fake gamblerA match1 |> number "resultVersion" |> shouldEqual (Some 2)
            detail harness.Fake gamblerA match1 |> text "computedDateTime" |> shouldEqual computedAt
            membership harness.Fake gamblerA |> number "scoreVersion" |> shouldEqual (versionBefore |> Option.map ((+) 1))

            // Delivered again, and the original result replayed: nothing changes.
            harness.Commands.ExpireDeduplication()
            let! _ = harness.Dispatch.ExecuteAsync(Ulid.newOf match1, { HomeTeamScore = 2; AwayTeamScore = 0; Version = 2 })
            let! _ = harness.Dispatch.ExecuteAsync(Ulid.newOf match1, { HomeTeamScore = 1; AwayTeamScore = 0; Version = 1 })
            do! harness.RunUntilQuiescentAsync()

            [ total harness.Fake gamblerA; total harness.Fake gamblerB ] |> shouldEqual [ Some 18; Some 14 ]
            award harness.Fake gamblerA match1 |> shouldEqual (Some 14)
        }

    [<Fact>]
    let ``a correction that skips its prerequisite version is rejected before any pool work`` () =
        async {
            let! harness = migrated ()
            do! harness.SettleAsync(match1, 1, 0)

            let! accepted =
                harness.LayoutRepository.CorrectMatchResultAsync(Ulid.newOf poolLayoutId, Ulid.newOf match1, 3, 0, 2)

            accepted |> shouldEqual false
            harness.Commands.Pending |> shouldEqual []
            (root harness.Fake |> Option.get).ContainsKey "activeOperation" |> shouldEqual false
            total harness.Fake gamblerA |> shouldEqual (Some 24)
        }

    [<Fact>]
    let ``a correction interrupted by a failed write converges without double counting`` () =
        async {
            let! harness = migrated ()
            do! harness.SettleAsync(match1, 1, 0)
            let fired = ref false

            harness.Fake.Intercept(fun request ->
                if not fired.Value && touchesDetail gamblerB match1 request then
                    fired.Value <- true
                    FailBefore(InternalServerErrorException "Internal server error")
                else
                    Proceed)

            let! _ = harness.CorrectResultAsync(match1, 2, 0)
            do! harness.RunUntilQuiescentAsync()

            fired.Value |> shouldEqual true
            [ total harness.Fake gamblerA; total harness.Fake gamblerB; total harness.Fake gamblerC ] |> shouldEqual [ Some 18; Some 14; Some 6 ]
        }

    [<Fact>]
    let ``a late join after settlement follows the late-join rule without inventing a prediction`` () =
        async {
            let! harness = migrated ()
            do! harness.SettleAsync(match1, 1, 0)

            let! _ = harness.JoinAsync gamblerD
            do! harness.RunUntilQuiescentAsync()

            let late = detail harness.Fake gamblerD match1
            late |> has "computedRequestId" |> shouldEqual true
            late |> number "score" |> shouldEqual (Some 0)
            late |> number "resultVersion" |> shouldEqual (Some 1)
            late |> has "homeTeamBet" |> shouldEqual false
            detail harness.Fake gamblerD match2 |> has "computedRequestId" |> shouldEqual false
            total harness.Fake gamblerD |> shouldEqual (Some 0)
            position harness.Fake gamblerD |> shouldEqual (Some 4)
        }

    [<Fact>]
    let ``a result that lands while a joining member's detail is created is reconciled`` () =
        async {
            let! harness = migrated ()
            let raced = ref false

            harness.Fake.Intercept(fun request ->
                if not raced.Value && touchesDetail gamblerD match2 request then
                    raced.Value <- true
                    // The result is published after the join read the layout but before its
                    // uncomputed detail is written; the dispatch's index read misses it.
                    harness.SetLayoutResult(match2, 0, 0)
                    Proceed
                else
                    Proceed)

            let! _ = harness.JoinAsync gamblerD
            do! harness.RunUntilQuiescentAsync()

            raced.Value |> shouldEqual true
            isComputed harness.Fake gamblerD match2 |> shouldEqual true
            award harness.Fake gamblerD match2 |> shouldEqual (Some 0)
            // Every other member of the pool is settled by the same reconciliation.
            [ total harness.Fake gamblerA; total harness.Fake gamblerB; total harness.Fake gamblerC ] |> shouldEqual [ Some 24; Some 14; Some 6 ]
        }

    [<Fact>]
    let ``an insertion and its result converge while the match index lags`` () =
        async {
            let! harness = migrated ()
            harness.Fake.FreezeIndexes()

            let added =
                { MatchId = match3
                  MatchDateTime = pastKickoff.AddHours 1.0
                  Result = None }

            do! harness.HandleLayoutRecordsAsync [ MatchInserted added; ResultChanged(match3, 1, 1) ]
            do! harness.RunUntilQuiescentAsync()
            harness.Fake.ReleaseIndexes()

            [ gamblerA; gamblerB; gamblerC ]
            |> List.map (fun gamblerId -> isComputed harness.Fake gamblerId match3)
            |> shouldEqual [ true; true; true ]
        }

    [<Fact>]
    let ``a total that changes before a new detail is written is never left stale`` () =
        async {
            let! harness = migrated ()
            do! harness.SettleAsync(match1, 1, 0)
            let raced = ref false

            harness.Fake.Intercept(fun request ->
                match request with
                | :? TransactWriteItemsRequest as transaction when
                    not raced.Value
                    && transaction.TransactItems |> Seq.exists (fun item -> not (isNull item.Put))
                    && touchesDetail gamblerA match3 request
                    ->
                    raced.Value <- true
                    // A score settles for A after the materializer read A's binding.
                    let current = membership harness.Fake gamblerA |> Option.get
                    current["score"] <- AttributeValue(N = "30")
                    current["scoreVersion"] <- AttributeValue(N = string (int current["scoreVersion"].N + 1))
                    harness.Fake.Put("Pool", current)
                    Proceed
                | _ -> Proceed)

            do!
                harness.HandleLayoutRecordsAsync
                    [ MatchInserted
                          { MatchId = match3
                            MatchDateTime = pastKickoff.AddHours 1.0
                            Result = None } ]

            raced.Value |> shouldEqual true
            detail harness.Fake gamblerA match3 |> number "totalPoolPoints" |> shouldEqual (Some 30)

            detail harness.Fake gamblerA match3 |> number "poolScoreVersion"
            |> shouldEqual (membership harness.Fake gamblerA |> number "scoreVersion")
        }
