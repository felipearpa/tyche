namespace Felipearpa.Tyche.Pool.Test.Settlement

open Xunit

/// The failure-path scenarios against the versioned, ordered settlement flow.
module SettlementFailurePathTest =

    let private run scenario =
        scenario (VersionedSettlementHarness() :> ISettlementHarness) |> Async.RunSynchronously

    [<Fact>]
    let ``lost post-commit notification`` () = run SettlementScenarios.lostPostCommitNotification

    [<Fact>]
    let ``cancellation is not settlement`` () = run SettlementScenarios.cancellationIsNotSettlement

    [<Fact>]
    let ``partial position write keeps previous positions`` () =
        run SettlementScenarios.partialPositionWriteKeepsPreviousPositions

    [<Fact>]
    let ``older operation replay is a no-op`` () = run SettlementScenarios.olderOperationReplayIsNoOp

    [<Fact>]
    let ``positions ignore a lagging leaderboard index`` () =
        run SettlementScenarios.positionsIgnoreLaggingLeaderboardIndex

    [<Fact>]
    let ``match inserted with its result is settled`` () =
        run SettlementScenarios.matchInsertedWithResultIsSettled

    [<Fact>]
    let ``insertion and result in one delivery converge`` () =
        run SettlementScenarios.insertionAndResultInOneDeliveryConverge

    [<Fact>]
    let ``interrupted settlement resumes`` () = run SettlementScenarios.interruptedSettlementResumes
