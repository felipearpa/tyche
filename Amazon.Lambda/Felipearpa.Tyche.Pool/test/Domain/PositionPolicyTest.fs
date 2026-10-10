namespace Felipearpa.Tyche.Pool.Test.Domain

open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type
open FsUnitTyped
open Xunit

module PositionPolicyTest =

    let private membership (gamblerId: string) total : MembershipState =
        { GamblerId = Ulid.newOf gamblerId
          GamblerUsername = "u"
          GamblerEmail = "u@example.com"
          PoolLayoutId = Ulid.newOf "01K1PX1TX2NM1HG851S1V0Y001"
          Generation = Some "G"
          ScoreVersion = Some 1L
          Total = total
          BeforeScore = None
          Position = None
          BeforePosition = None
          PositionOperationRevision = None
          PositionCalculationRevision = None }

    let private settlement =
        MatchSettlement(
            Ulid.newOf "01K1PX1TX2NM1HG851S1V0M001",
            { HomeTeamScore = 1
              AwayTeamScore = 0
              Version = 1 }
        )

    [<Fact>]
    let ``members rank by total descending, then gambler ID, with missing totals last`` () =
        [ membership "01K1PX1TX2NM1HG851S1V0GD01" None
          membership "01K1PX1TX2NM1HG851S1V0GC01" (Some 10)
          membership "01K1PX1TX2NM1HG851S1V0GB01" (Some 0)
          membership "01K1PX1TX2NM1HG851S1V0GA01" (Some 10) ]
        |> PositionPolicy.rank
        |> List.map (fun (m, position) -> m.GamblerId.Value.Substring(22), position)
        |> shouldEqual [ "GA01", 1; "GC01", 2; "GB01", 3; "GD01", 4 ]

    [<Fact>]
    let ``the first write of a match settlement records the previous position`` () =
        let current = { membership "01K1PX1TX2NM1HG851S1V0GA01" (Some 5) with Position = Some 3 }

        PositionPolicy.decide settlement 7L current 1
        |> shouldEqual (
            WritePosition
                { Position = 1
                  FirstWrite = true
                  MembershipBeforePosition = Some 3
                  DetailBeforePosition = Some 3 }
        )

    [<Fact>]
    let ``a standings reconciliation moves the position without restarting the trend`` () =
        let current =
            { membership "01K1PX1TX2NM1HG851S1V0GA01" (Some 5) with
                Position = Some 3
                BeforePosition = Some 2
                PositionOperationRevision = Some 7L }

        PositionPolicy.decide CurrentStandings 9L current 2
        |> shouldEqual (
            WritePosition
                { Position = 2
                  FirstWrite = true
                  MembershipBeforePosition = None
                  DetailBeforePosition = Some 3 }
        )

        match PositionPolicy.decide CurrentStandings 9L (membership "01K1PX1TX2NM1HG851S1V0GD01" (Some 0)) 4 with
        | WritePosition change -> change.MembershipBeforePosition |> shouldEqual None
        | other -> failwith $"unexpected {other}"

    [<Fact>]
    let ``a recalculation within the operation keeps the recorded previous position`` () =
        let written =
            { membership "01K1PX1TX2NM1HG851S1V0GA01" (Some 5) with
                Position = Some 1
                BeforePosition = Some 3
                PositionOperationRevision = Some 7L }

        match PositionPolicy.decide settlement 7L written 2 with
        | WritePosition change ->
            change.FirstWrite |> shouldEqual false
            change.Position |> shouldEqual 2
            change.MembershipBeforePosition |> shouldEqual None
            change.DetailBeforePosition |> shouldEqual (Some 3)
        | other -> failwith $"unexpected {other}"

        PositionPolicy.decide settlement 7L written 1 |> shouldEqual PositionCurrent

    [<Fact>]
    let ``an older operation never overwrites a newer operation's position`` () =
        let newer = { membership "01K1PX1TX2NM1HG851S1V0GA01" (Some 5) with PositionOperationRevision = Some 8L }
        PositionPolicy.decide settlement 7L newer 1 |> shouldEqual PositionSuperseded

    [<Fact>]
    let ``a membership without an earlier position records the new one as previous`` () =
        match PositionPolicy.decide settlement 1L (membership "01K1PX1TX2NM1HG851S1V0GA01" (Some 0)) 4 with
        | WritePosition change ->
            change.MembershipBeforePosition |> shouldEqual (Some 4)
            change.DetailBeforePosition |> shouldEqual None
        | other -> failwith $"unexpected {other}"
