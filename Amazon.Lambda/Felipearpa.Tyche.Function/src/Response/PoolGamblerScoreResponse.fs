namespace Felipearpa.Tyche.Function.Response

open System

/// The pool's trend kickoff group: the newest kickoff with a settled match, whose baseline
/// `beforePosition` holds.
type TrendGroupResponse =
    { /// The group's kickoff instant in UTC; clients group by this value.
      KickoffDateTime: DateTime
      ComputedMatches: int
      TotalMatches: int }

type PoolGamblerScoreResponse =
    { PoolId: string
      PoolName: string
      GamblerId: string
      GamblerUsername: string
      Position: int option
      BeforePosition: int option
      Score: int option
      BeforeScore: int option
      GamblerCount: int option
      /// None (null) while the pool has no settled kickoff group.
      TrendGroup: TrendGroupResponse option }
