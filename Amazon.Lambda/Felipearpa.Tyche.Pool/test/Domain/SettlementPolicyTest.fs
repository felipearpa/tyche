namespace Felipearpa.Tyche.Pool.Test.Domain

open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Pool.Type
open Felipearpa.Type
open FsUnitTyped
open Xunit

module SettlementPolicyTest =

    let private gamblerId = Ulid.newOf "01K1PX1TX2NM1HG851S1V0GA01"
    let private matchId = Ulid.newOf "01K1PX1TX2NM1HG851S1V0M001"

    let private membership total : MembershipState =
        { GamblerId = gamblerId
          GamblerUsername = "a"
          GamblerEmail = "a@example.com"
          PoolLayoutId = Ulid.newOf "01K1PX1TX2NM1HG851S1V0Y001"
          Generation = Some "G1"
          ScoreVersion = Some 3L
          Total = Some total
          BeforeScore = Some total
          Position = Some 1
          BeforePosition = None
          PositionOperationRevision = None
          PositionCalculationRevision = None }

    let private bet prediction : BetSettlementState =
        { GamblerId = gamblerId
          MatchId = matchId
          Generation = Some "G1"
          Prediction =
            prediction
            |> Option.map (fun (home, away) ->
                { TeamScore.HomeTeamValue = BetScore.newOf home
                  AwayTeamValue = BetScore.newOf away })
          Award = None
          ResultVersion = None
          IsComputed = false
          PositionOperationRevision = None }

    let private result home away version =
        { HomeTeamScore = home
          AwayTeamScore = away
          Version = version }

    [<Fact>]
    let ``a first settlement adds the award and expects an unsettled detail`` () =
        match SettlementPolicy.decide (membership 100) (bet (Some(1, 0))) (result 1 0 1) with
        | Apply change ->
            change.Award |> shouldEqual 20
            change.Total |> shouldEqual 120
            change.Expectation |> shouldEqual Unsettled
            change.ExpectedScoreVersion |> shouldEqual 3L
            change.TotalChanged |> shouldEqual true
        | other -> failwith $"unexpected {other}"

    [<Fact>]
    let ``a missing prediction settles with a confirmed zero award and an unchanged total`` () =
        match SettlementPolicy.decide (membership 100) (bet None) (result 1 0 1) with
        | Apply change ->
            change.Award |> shouldEqual 0
            change.TotalChanged |> shouldEqual false
        | other -> failwith $"unexpected {other}"

    [<Fact>]
    let ``a correction applies only the difference from the previously applied award`` () =
        let settled =
            { bet (Some(1, 0)) with
                IsComputed = true
                Award = Some 10
                ResultVersion = Some 1 }

        match SettlementPolicy.decide (membership 100) settled (result 3 0 2) with
        | Apply change ->
            change.Award |> shouldEqual 14
            change.Total |> shouldEqual 104
            change.Expectation |> shouldEqual (SettledAt 1)
        | other -> failwith $"unexpected {other}"

    [<Fact>]
    let ``a correction that lowers the award lowers the total`` () =
        let settled =
            { bet (Some(1, 0)) with
                IsComputed = true
                Award = Some 20
                ResultVersion = Some 1 }

        match SettlementPolicy.decide (membership 120) settled (result 2 0 2) with
        | Apply change ->
            change.Award |> shouldEqual 14
            change.Total |> shouldEqual 114
        | other -> failwith $"unexpected {other}"

    [<Fact>]
    let ``an equal or newer applied version is never applied again`` () =
        let settled =
            { bet (Some(1, 0)) with
                IsComputed = true
                Award = Some 20
                ResultVersion = Some 2 }

        SettlementPolicy.decide (membership 120) settled (result 1 0 2) |> shouldEqual AlreadyApplied
        SettlementPolicy.decide (membership 120) settled (result 1 0 1) |> shouldEqual AlreadyApplied

    [<Fact>]
    let ``a detail computed before versioning counts as result version 1`` () =
        let legacy =
            { bet (Some(1, 0)) with
                IsComputed = true
                Award = Some 20 }

        SettlementPolicy.decide (membership 120) legacy (result 1 0 1) |> shouldEqual AlreadyApplied

        match SettlementPolicy.decide (membership 120) legacy (result 0 0 2) with
        | Apply change -> change.Expectation |> shouldEqual LegacySettled
        | other -> failwith $"unexpected {other}"

    [<Fact>]
    let ``details of another membership generation or without identity are not settled`` () =
        SettlementPolicy.decide (membership 100) { bet None with Generation = Some "G0" } (result 1 0 1)
        |> shouldEqual ObsoleteMembership

        SettlementPolicy.decide (membership 100) { bet None with Generation = None } (result 1 0 1)
        |> shouldEqual IdentityUnbound

        SettlementPolicy.decide { membership 100 with Generation = None } (bet None) (result 1 0 1)
        |> shouldEqual IdentityUnbound

    [<Fact>]
    let ``a computed detail without an award is held as malformed`` () =
        match SettlementPolicy.decide (membership 100) { bet None with IsComputed = true } (result 1 0 2) with
        | Malformed _ -> ()
        | other -> failwith $"unexpected {other}"
