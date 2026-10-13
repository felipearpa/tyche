namespace Felipearpa.Tyche.Pool.Domain

open System
open Felipearpa.Type

/// What a pool root holds for rank movement: the layout its kickoff groups come from and the
/// kickoff of the group whose baseline the memberships hold.
type PoolTrend =
    { PoolId: Ulid
      PoolLayoutId: Ulid
      /// None while no match settlement has completed for the pool.
      TrendKickoffDateTime: DateTime option }

type PoolGamblerScore =
    { PoolId: Ulid
      PoolName: NonEmptyString100
      GamblerId: Ulid
      GamblerUsername: NonEmptyString100
      Position: int option
      BeforePosition: int option
      Score: int option
      BeforeScore: int option
      GamblerCount: int option }

/// A score row with its pool's trend kickoff group, the group its movement measures.
type PoolStanding =
    { Score: PoolGamblerScore
      /// None while the pool has no settled kickoff group.
      TrendGroup: KickoffGroup option }
