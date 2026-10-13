namespace Felipearpa.Tyche.Pool.Domain

open System

/// A position transition for one membership within an operation.
type PositionChange =
    { Position: int
      /// True for the operation's first write to this membership, which stamps its operation
      /// marker. Later recalculations in the same operation keep it.
      FirstWrite: bool
      /// The previous position to record on the membership as the baseline of a newer kickoff
      /// group, so the leaderboard trend compares with the position before that group. None
      /// leaves it untouched: recalculations keep what the first write recorded, a settlement
      /// within the trend group or an older one keeps the group's baseline, and a standings
      /// reconciliation moves positions without restarting the trend.
      MembershipBeforePosition: int option
      /// The previous position copied to the target match detail; None when the membership
      /// had no position before this operation.
      DetailBeforePosition: int option }

type PositionDecision =
    /// The membership already holds this position for the operation.
    | PositionCurrent
    /// A newer operation already wrote this membership's position.
    | PositionSuperseded
    | WritePosition of PositionChange

/// Pure position calculation over authoritative membership totals.
module PositionPolicy =

    /// Orders members by total descending and gambler ID ascending and numbers them from 1.
    /// Members without a total, which only malformed data produces, rank last.
    let rank (members: MembershipState list) : (MembershipState * int) list =
        members
        |> List.sortWith (fun left right ->
            match left.Total, right.Total with
            | Some l, Some r when l <> r -> compare r l
            | Some _, None -> -1
            | None, Some _ -> 1
            | _ -> String.CompareOrdinal(left.GamblerId.Value, right.GamblerId.Value))
        |> List.mapi (fun index membership -> membership, index + 1)

    /// Compares the settling match's kickoff with the pool's trend kickoff, both as UTC
    /// instants. A newer kickoff, or a pool without a trend kickoff, opens a group; the same or
    /// an older kickoff keeps the current one.
    let trendEffect (trendKickoff: DateTime option) (kickoff: DateTime option) =
        match kickoff |> Option.map KickoffGroup.instant, trendKickoff |> Option.map KickoffGroup.instant with
        | None, _ -> Ungrouped
        | Some kickoff, Some trend when kickoff <= trend -> KeepsGroup
        | Some kickoff, _ -> OpensGroup kickoff

    /// The trend kickoff the pool root takes when the operation completes; None keeps the
    /// current one.
    let completedTrendKickoff (target: OperationTarget) =
        match target with
        | MatchSettlement(_, _, OpensGroup kickoff) -> Some kickoff
        | MatchSettlement _
        | CurrentStandings
        | RejoinRestoration _ -> None

    /// Decides the write for one membership. Only a match settlement that opens a newer
    /// kickoff group, or whose kickoff is unknown, records the previous position. A settlement
    /// within the trend group or an older one, a standings reconciliation after a join or
    /// removal, and a rejoin restoration leave the baseline as the group's first settlement
    /// recorded it.
    let decide (target: OperationTarget) (operationRevision: int64) (membership: MembershipState) (position: int) =
        let baseline value =
            match target with
            | MatchSettlement(_, _, OpensGroup _)
            | MatchSettlement(_, _, Ungrouped) -> Some value
            | MatchSettlement(_, _, KeepsGroup)
            | CurrentStandings
            | RejoinRestoration _ -> None

        match membership.PositionOperationRevision with
        | Some written when written > operationRevision -> PositionSuperseded
        | Some written when written = operationRevision ->
            if membership.Position = Some position then
                PositionCurrent
            else
                WritePosition
                    { Position = position
                      FirstWrite = false
                      MembershipBeforePosition = None
                      DetailBeforePosition = membership.BeforePosition }
        | _ ->
            WritePosition
                { Position = position
                  FirstWrite = true
                  MembershipBeforePosition = baseline (membership.Position |> Option.defaultValue position)
                  DetailBeforePosition = membership.Position }
