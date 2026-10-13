namespace Felipearpa.Tyche.Pool.Test.Domain

open System
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

    let private kickoff = DateTime(2026, 6, 12, 14, 0, 0, DateTimeKind.Utc)

    let private settlementWith trend =
        MatchSettlement(
            Ulid.newOf "01K1PX1TX2NM1HG851S1V0M001",
            { HomeTeamScore = 1
              AwayTeamScore = 0
              Version = 1 },
            trend
        )

    let private settlement = settlementWith (OpensGroup kickoff)

    /// Applies a decided write to the membership as the guarded position write stores it.
    let private apply (revision: int64) (decision: PositionDecision) (current: MembershipState) =
        match decision with
        | WritePosition change ->
            { current with
                Position = Some change.Position
                BeforePosition = change.MembershipBeforePosition |> Option.orElse current.BeforePosition
                PositionOperationRevision =
                    if change.FirstWrite then Some revision else current.PositionOperationRevision }
        | PositionCurrent
        | PositionSuperseded -> current

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

    [<Fact>]
    let ``a newer kickoff, or a pool without a trend kickoff, opens a group`` () =
        PositionPolicy.trendEffect None (Some kickoff) |> shouldEqual (OpensGroup kickoff)

        PositionPolicy.trendEffect (Some kickoff) (Some(kickoff.AddHours 4.0))
        |> shouldEqual (OpensGroup(kickoff.AddHours 4.0))

    [<Fact>]
    let ``the same kickoff in another offset, or an older kickoff, keeps the group`` () =
        let sameInstant = DateTimeOffset(2026, 6, 12, 9, 0, 0, TimeSpan.FromHours -5.0).UtcDateTime
        let sameInstantLocal = kickoff.ToLocalTime()

        PositionPolicy.trendEffect (Some kickoff) (Some sameInstant) |> shouldEqual KeepsGroup
        PositionPolicy.trendEffect (Some kickoff) (Some sameInstantLocal) |> shouldEqual KeepsGroup
        PositionPolicy.trendEffect (Some kickoff) (Some(kickoff.AddDays -1.0)) |> shouldEqual KeepsGroup

    [<Fact>]
    let ``an unknown kickoff leaves the trend kickoff unchanged`` () =
        PositionPolicy.trendEffect (Some kickoff) None |> shouldEqual Ungrouped
        PositionPolicy.completedTrendKickoff (settlementWith Ungrouped) |> shouldEqual None
        PositionPolicy.completedTrendKickoff (settlementWith KeepsGroup) |> shouldEqual None
        PositionPolicy.completedTrendKickoff CurrentStandings |> shouldEqual None
        PositionPolicy.completedTrendKickoff settlement |> shouldEqual (Some kickoff)

    [<Fact>]
    let ``two settlements of one group keep the baseline taken before the group`` () =
        // Eighth before the group; the first match leaves the gambler fifth, the second sixth.
        let eighth = { membership "01K1PX1TX2NM1HG851S1V0GA01" (Some 5) with Position = Some 8; BeforePosition = Some 4; PositionOperationRevision = Some 3L }

        let afterFirst = eighth |> apply 4L (PositionPolicy.decide (settlementWith (OpensGroup kickoff)) 4L eighth 5)
        (afterFirst.BeforePosition, afterFirst.Position) |> shouldEqual (Some 8, Some 5)

        let secondDecision = PositionPolicy.decide (settlementWith KeepsGroup) 5L afterFirst 6
        let afterSecond = afterFirst |> apply 5L secondDecision
        (afterSecond.BeforePosition, afterSecond.Position) |> shouldEqual (Some 8, Some 6)

        match secondDecision with
        | WritePosition change ->
            // The settled match detail still gets its own snapshot: fifth before, sixth after.
            change.DetailBeforePosition |> shouldEqual (Some 5)
            change.MembershipBeforePosition |> shouldEqual None
        | other -> failwith $"unexpected {other}"

    [<Fact>]
    let ``a newer kickoff resets the baseline to the position before its first settlement`` () =
        let held = { membership "01K1PX1TX2NM1HG851S1V0GA01" (Some 5) with Position = Some 6; BeforePosition = Some 8; PositionOperationRevision = Some 5L }

        let next = held |> apply 6L (PositionPolicy.decide (settlementWith (OpensGroup(kickoff.AddHours 4.0))) 6L held 2)
        (next.BeforePosition, next.Position) |> shouldEqual (Some 6, Some 2)

    [<Fact>]
    let ``a late result or correction of an older group keeps the baseline`` () =
        let held = { membership "01K1PX1TX2NM1HG851S1V0GA01" (Some 5) with Position = Some 6; BeforePosition = Some 8; PositionOperationRevision = Some 5L }

        match PositionPolicy.decide (settlementWith KeepsGroup) 6L held 3 with
        | WritePosition change ->
            change.FirstWrite |> shouldEqual true
            change.Position |> shouldEqual 3
            change.MembershipBeforePosition |> shouldEqual None
            change.DetailBeforePosition |> shouldEqual (Some 6)
        | other -> failwith $"unexpected {other}"

    [<Fact>]
    let ``a resumed operation does not reset a membership it already wrote`` () =
        let before = { membership "01K1PX1TX2NM1HG851S1V0GA01" (Some 5) with Position = Some 8; PositionOperationRevision = Some 3L }
        let opening = settlementWith (OpensGroup kickoff)
        let written = before |> apply 4L (PositionPolicy.decide opening 4L before 5)

        // The resumed worker recalculates under the same stored decision: the baseline stays 8.
        match PositionPolicy.decide opening 4L written 7 with
        | WritePosition change ->
            change.FirstWrite |> shouldEqual false
            change.MembershipBeforePosition |> shouldEqual None
        | other -> failwith $"unexpected {other}"

        PositionPolicy.decide opening 4L written 5 |> shouldEqual PositionCurrent
