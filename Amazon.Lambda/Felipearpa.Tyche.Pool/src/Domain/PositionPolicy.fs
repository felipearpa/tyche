namespace Felipearpa.Tyche.Pool.Domain

open System

/// A position transition for one membership within an operation.
type PositionChange =
    { Position: int
      /// True for the operation's first write to this membership, which stamps its operation
      /// marker. Later recalculations in the same operation keep it.
      FirstWrite: bool
      /// The previous position to record on the membership, so the leaderboard trend compares
      /// with the last scored match. None leaves it untouched: recalculations keep what the
      /// first write recorded, and a standings reconciliation moves positions without
      /// restarting the trend.
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

    /// Decides the write for one membership. Only a match settlement or correction records
    /// the previous position; a standings reconciliation after a join or removal leaves it
    /// as the last scored match left it.
    let decide (target: OperationTarget) (operationRevision: int64) (membership: MembershipState) (position: int) =
        let baseline value =
            match target with
            | MatchSettlement _ -> Some value
            | CurrentStandings -> None

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
