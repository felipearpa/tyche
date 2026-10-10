namespace Felipearpa.Tyche.Pool.Test.Domain

open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Type
open Felipearpa.Type
open FsUnitTyped
open Xunit

module RestorePolicyTest =

    let private gamblerId = Ulid.newOf "01K1PX1TX2NM1HG851S1V0GD01"
    let private matchId = Ulid.newOf "01K1PX1TX2NM1HG851S1V0M001"

    let private membership : MembershipState =
        { GamblerId = gamblerId
          GamblerUsername = "d"
          GamblerEmail = "d@example.com"
          PoolLayoutId = Ulid.newOf "01K1PX1TX2NM1HG851S1V0Y001"
          Generation = Some "NEW"
          ScoreVersion = Some 2L
          Total = Some 4
          BeforeScore = None
          Position = Some 3
          BeforePosition = None
          PositionOperationRevision = None
          PositionCalculationRevision = None }

    /// A tombstone of the earlier generation that predicted 3-1 and earned 10 on version 1.
    let private retained : RetainedBetState =
        { Settlement =
            { GamblerId = gamblerId
              MatchId = matchId
              Generation = Some "OLD"
              Prediction =
                Some
                    { TeamScore.HomeTeamValue = BetScore.newOf 3
                      AwayTeamValue = BetScore.newOf 1 }
              Award = Some 10
              ResultVersion = Some 1
              IsComputed = true
              PositionOperationRevision = Some 7L }
          RestoreExcludedGeneration = None
          SettlementOperationId = Some "RESULT#M1#1" }

    let private scored home away version =
        MatchScored
            { HomeTeamScore = home
              AwayTeamScore = away
              Version = version }

    let private rebindOf decision =
        match decision with
        | Rebind change -> change
        | other -> failwith $"expected a rebinding, got {other}"

    [<Fact>]
    let ``a detail of the current result keeps its award and adds it to the total`` () =
        let change = RestorePolicy.decide "NEW" (Some membership) retained (scored 1 0 1) |> rebindOf

        change.Award |> shouldEqual 10
        change.Reevaluated |> shouldEqual None
        change.Total |> shouldEqual 14
        change.ScoreVersion |> shouldEqual 3L
        change.ReadGeneration |> shouldEqual (Some "OLD")
        change.ResultVersion |> shouldEqual 1

    [<Fact>]
    let ``a detail of an older result is re-evaluated against the current one`` () =
        // Corrected 1-0 → 2-2: the 3-1 prediction now earns nothing.
        let change = RestorePolicy.decide "NEW" (Some membership) retained (scored 2 2 2) |> rebindOf

        change.Award |> shouldEqual 0
        change.ReadAward |> shouldEqual 10
        change.Reevaluated |> shouldEqual (Some { HomeTeamScore = 2; AwayTeamScore = 2; Version = 2 })
        change.TotalChanged |> shouldEqual false
        change.ScoreVersion |> shouldEqual 2L

    [<Fact>]
    let ``a detail scored before versions reflects version 1`` () =
        let legacy =
            { retained with
                Settlement = { retained.Settlement with Generation = None; ResultVersion = None } }

        let change = RestorePolicy.decide "NEW" (Some membership) legacy (scored 1 0 1) |> rebindOf

        change.ReadGeneration |> shouldEqual None
        change.ReadResultVersion |> shouldEqual None
        change.ResultVersion |> shouldEqual 1
        change.Award |> shouldEqual 10

    [<Fact>]
    let ``details that cannot be scored are held for repair`` () =
        RestorePolicy.decide "NEW" (Some membership) retained MatchMissing |> shouldEqual (HoldForRepair "match missing")
        RestorePolicy.decide "NEW" (Some membership) retained MatchUnscored |> shouldEqual (HoldForRepair "match has no result")

        RestorePolicy.decide "NEW" (Some membership) { retained with Settlement = { retained.Settlement with Award = None } } (scored 1 0 1)
        |> shouldEqual (HoldForRepair "computed detail without an award")

        RestorePolicy.decide "NEW" (Some membership) { retained with Settlement = { retained.Settlement with ResultVersion = Some 3 } } (scored 1 0 1)
        |> shouldEqual (HoldForRepair "detail reflects a newer result than its match")

    [<Fact>]
    let ``restored, held, and uncomputed details need no write`` () =
        RestorePolicy.decide "NEW" (Some membership) { retained with Settlement = { retained.Settlement with Generation = Some "NEW" } } (scored 1 0 1)
        |> shouldEqual AlreadyRestored

        RestorePolicy.decide "NEW" (Some membership) { retained with RestoreExcludedGeneration = Some "NEW" } (scored 1 0 1)
        |> shouldEqual AlreadyHeld

        RestorePolicy.decide "NEW" (Some membership) { retained with Settlement = { retained.Settlement with IsComputed = false } } (scored 1 0 1)
        |> shouldEqual NotRetained

    [<Fact>]
    let ``a hold for an earlier generation does not hold the detail for a later one`` () =
        RestorePolicy.decide "NEW" (Some membership) { retained with RestoreExcludedGeneration = Some "OLDER" } (scored 1 0 1)
        |> rebindOf
        |> _.Total
        |> shouldEqual 14

    [<Fact>]
    let ``nothing is added to a membership that ended or lacks a total`` () =
        RestorePolicy.decide "NEW" None retained (scored 1 0 1) |> shouldEqual MembershipEnded

        RestorePolicy.decide "NEW" (Some { membership with Generation = Some "NEWER" }) retained (scored 1 0 1)
        |> shouldEqual MembershipEnded

        RestorePolicy.decide "NEW" (Some { membership with Total = None }) retained (scored 1 0 1)
        |> shouldEqual (InvalidMembership "membership without a score version or total")
