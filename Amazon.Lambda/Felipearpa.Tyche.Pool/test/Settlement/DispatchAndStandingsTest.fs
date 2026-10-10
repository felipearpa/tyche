namespace Felipearpa.Tyche.Pool.Test.Settlement

open System
open Amazon.DynamoDBv2.Model
open Felipearpa.Tyche.Pool.Application
open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Test.Fakes
open Felipearpa.Tyche.Pool.Test.Settlement.PoolScenario
open Felipearpa.Type
open FsUnitTyped
open Xunit

/// Result dispatch discovery and current-standings reconciliation.
module DispatchAndStandingsTest =

    let private pool = Ulid.newOf poolId

    let private result home away version =
        { HomeTeamScore = home
          AwayTeamScore = away
          Version = version }

    let private migrated () =
        async {
            let harness = VersionedSettlementHarness()
            do! harness.SeedAndMigrateAsync(defaultMembers, defaultMatches)
            return harness
        }

    /// Only the discovery page access the dispatcher uses; other members are not called.
    let private pagedDiscovery (pages: (Ulid list * string option) list) =
        let remaining = ref pages

        { new IPoolSettlementRepository with
            member _.GetPoolsWithMatchPageAsync(_, _) =
                async {
                    let page = remaining.Value.Head
                    remaining.Value <- remaining.Value.Tail
                    return page
                }

            member _.GetRootAsync _ = failwith "not used"
            member _.GetReceiptAsync(_, _) = failwith "not used"
            member _.AdmitAsync(_, _, _) = failwith "not used"
            member _.RecordCompletedAsync(_, _) = failwith "not used"
            member _.RecordDeferredAsync(_, _) = failwith "not used"
            member _.GetDeferredReceiptsAsync _ = failwith "not used"
            member _.ReplaceCheckpointAsync(_, _, _) = failwith "not used"
            member _.CompleteAsync(_, _, _) = failwith "not used"
            member _.GetMembershipPageAsync(_, _, _) = failwith "not used"
            member _.GetMembershipAsync(_, _) = failwith "not used"
            member _.GetBetSettlementStatesAsync(_, _, _) = failwith "not used"
            member _.GetBetSettlementStateAsync(_, _, _) = failwith "not used"
            member _.ApplySettlementAsync _ = failwith "not used"
            member _.ApplyPositionAsync _ = failwith "not used" }

    let private recordingPublisher () =
        let published = ref []

        { new IPoolCommandPublisher with
            member _.PublishAsync(commands) = async { published.Value <- published.Value @ commands }
            member _.RepublishAsync(commands) = async { published.Value <- published.Value @ commands } },
        published

    [<Fact>]
    let ``dispatch walks every page, including empty pages with a continuation`` () =
        async {
            let poolA = Ulid.newOf "01K1PX1TX2NM1HG851S1V0P001"
            let poolB = Ulid.newOf "01K1PX1TX2NM1HG851S1V0P002"
            let publisher, published = recordingPublisher ()

            let repository =
                pagedDiscovery [ [ poolA ], Some "c1"; [], Some "c2"; [ poolA; poolB ], None ]

            let! count = DispatchMatchResult(repository, publisher).ExecuteAsync(Ulid.newOf match1, result 1 0 1)

            count |> shouldEqual 2
            published.Value |> List.map _.PoolId |> shouldEqual [ poolA; poolB ]
            published.Value |> List.map _.OperationId |> List.distinct |> shouldEqual [ $"RESULT#{match1}#1" ]
        }

    [<Fact>]
    let ``a retry after a partially accepted dispatch reaches every pool, including settled ones`` () =
        async {
            let! harness = migrated ()

            // A second pool on the same match.
            let otherPool = "01K1PX1TX2NM1HG851S1V0P002"

            let otherDetail = detailItem gamblerD { MatchId = match1; MatchDateTime = pastKickoff; Result = None } None
            otherDetail["pk"] <- AttributeValue(S = $"GAMBLER#{gamblerD}#POOL#{otherPool}")
            otherDetail["poolId"] <- AttributeValue(S = otherPool)
            otherDetail["getPoolGamblerScoresByMatchSk"] <- AttributeValue(S = $"POOL#{otherPool}#GAMBLER#{gamblerD}")
            harness.Fake.Put("Pool", otherDetail)

            let sends = ref 0

            harness.Commands.FailSendWhen(fun _ ->
                sends.Value <- sends.Value + 1
                if sends.Value = 2 then Some(exn "SQS unavailable") else None)

            let! first = harness.Dispatch.ExecuteAsync(Ulid.newOf match1, result 1 0 1) |> Async.Catch
            first |> function Choice2Of2 _ -> () | _ -> failwith "expected the second enqueue to fail"
            harness.Commands.ClearSendFailures()

            // The accepted pool settles before the retry.
            do! harness.DeliverCommandsAsync()
            isComputed harness.Fake gamblerA match1 |> shouldEqual true

            let! count = harness.Dispatch.ExecuteAsync(Ulid.newOf match1, result 1 0 1)
            count |> shouldEqual 2
            harness.Commands.Pending |> List.map _.GroupId |> shouldEqual [ otherPool ]
        }

    [<Fact>]
    let ``a join before any result reconciles current positions without touching match details`` () =
        async {
            let! harness = migrated ()
            let! _ = harness.JoinAsync gamblerD
            do! harness.RunUntilQuiescentAsync()

            [ gamblerB; gamblerC; gamblerA; gamblerD ]
            |> List.map (position harness.Fake)
            |> shouldEqual [ Some 1; Some 2; Some 3; Some 4 ]

            // A new member has no trend until a match is scored.
            beforePosition harness.Fake gamblerD |> shouldEqual None

            [ gamblerA; gamblerB; gamblerC; gamblerD ]
            |> List.collect (fun g -> [ match1; match2 ] |> List.map (fun m -> detail harness.Fake g m |> has "position"))
            |> List.distinct
            |> shouldEqual [ false ]
        }

    [<Fact>]
    let ``a removal after a settled match updates current positions and keeps the match's positions`` () =
        async {
            let! harness = migrated ()
            do! harness.SettleAsync(match1, 1, 0)

            let match1Positions =
                [ gamblerA; gamblerB; gamblerC ]
                |> List.map (fun g -> detailPosition harness.Fake g match1, detailBeforePosition harness.Fake g match1)

            do! harness.RemoveAsync gamblerB
            do! harness.RunUntilQuiescentAsync()

            [ position harness.Fake gamblerA; position harness.Fake gamblerC ] |> shouldEqual [ Some 1; Some 2 ]

            // The trend still compares with the last scored match: A keeps its climb from 3 to 1,
            // and C's previous position is the one match1 left, not the one before the removal.
            beforePosition harness.Fake gamblerA |> shouldEqual (Some 3)
            beforePosition harness.Fake gamblerC |> shouldEqual (Some 2)

            [ gamblerA; gamblerB; gamblerC ]
            |> List.map (fun g -> detailPosition harness.Fake g match1, detailBeforePosition harness.Fake g match1)
            |> shouldEqual match1Positions
        }
