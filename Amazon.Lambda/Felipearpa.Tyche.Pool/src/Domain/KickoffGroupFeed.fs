namespace Felipearpa.Tyche.Pool.Domain

open System

/// A gambler's match detail as the grouped History and Timeline feeds read it.
type GroupedBet =
    { Bet: PoolGamblerBet
      /// The kickoff the read index orders the detail by: `computedMatchDateTime` on the
      /// finished-by-kickoff index, `matchDateTime` on the pending index.
      Kickoff: DateTime
      /// That kickoff exactly as stored, which the by-kickoff equality queries match.
      StoredKickoff: string
      ComputedDateTime: DateTime option
      /// The position snapshot settlement stored on the detail when it computed it.
      BeforePosition: int option
      Position: int option }

/// The gambler's rank movement across one kickoff group: the position before the group's
/// first computed match and the position after its latest one.
type GroupMovement = { BeforePosition: int; Position: int }

/// One item of a grouped feed: a kickoff group with its layout counts, the requested gambler's
/// movement in it, and that gambler's visible match rows.
type KickoffGroupItem =
    { Group: KickoffGroup
      Movement: GroupMovement option
      Bets: PoolGamblerBet list }

/// Which Timeline phase a kickoff group belongs to.
type TimelinePhase =
    /// Some layout match of the group has no result yet.
    | UnsettledGroup
    /// Every layout match of the group has a result.
    | SettledGroup

[<RequireQualifiedAccess>]
module KickoffGroupFeed =

    /// The movement from the stored snapshots of the group's computed children: the earliest
    /// computed child's `beforePosition` and the latest one's `position`, in computation order
    /// with the match ID breaking ties. None when no child is computed or either snapshot is
    /// missing; a missing snapshot is never filled from another source.
    let movement (children: GroupedBet list) : GroupMovement option =
        let computed =
            children
            |> List.filter _.Bet.isComputed
            |> List.sortBy (fun child -> child.ComputedDateTime, child.Bet.MatchId.Value)

        match computed with
        | [] -> None
        | earliest :: _ ->
            match earliest.BeforePosition, (List.last computed).Position with
            | Some beforePosition, Some position ->
                Some
                    { BeforePosition = beforePosition
                      Position = position }
            | _ -> None

    /// The feed item of one group's children, with the group's counts from its layout.
    let item (layoutGroups: Map<DateTime, KickoffGroup>) (kickoff: DateTime) (children: GroupedBet list) =
        { Group = layoutGroups |> KickoffGroup.at kickoff
          Movement = movement children
          Bets = children |> List.map _.Bet |> List.sortBy _.MatchId.Value }

    /// Splits index-ordered details into runs of one kickoff instant, keeping their order. A
    /// kickoff's details are adjacent in a by-kickoff index, so each run is one group, possibly
    /// cut short by a storage page boundary.
    let runs (bets: GroupedBet list) : (DateTime * GroupedBet list) list =
        bets
        |> List.fold
            (fun runs bet ->
                let kickoff = KickoffGroup.instant bet.Kickoff

                match runs with
                | (current, members) :: earlier when current = kickoff -> (current, bet :: members) :: earlier
                | _ -> (kickoff, [ bet ]) :: runs)
            []
        |> List.rev
        |> List.map (fun (kickoff, members) -> kickoff, List.rev members)

    /// The phase of a group by its layout counts. A kickoff the layout no longer has, such as
    /// one a match was moved away from, is unsettled until its details follow the move.
    let phase (group: KickoffGroup) =
        if group.TotalMatches > 0 && group.ComputedMatches = group.TotalMatches then
            SettledGroup
        else
            UnsettledGroup
