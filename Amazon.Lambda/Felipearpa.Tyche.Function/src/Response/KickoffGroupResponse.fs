namespace Felipearpa.Tyche.Function.Response

open System

/// The gambler's rank movement across a kickoff group, from the group's stored snapshots.
type GroupMovementResponse = { BeforePosition: int; Position: int }

/// One item of the grouped History and Timeline feeds.
type KickoffGroupResponse =
    { /// The group's kickoff instant in UTC; clients group by this value.
      KickoffDateTime: DateTime
      TotalMatches: int
      ComputedMatches: int
      /// None (null) when the gambler has no computed match in the group or a snapshot is missing.
      Movement: GroupMovementResponse option
      /// The group's visible match rows, by match ID.
      Matches: PoolGamblerBetResponse list }
