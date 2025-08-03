namespace Felipearpa.Tyche.Pool.Domain

open Felipearpa.Type

type ResolvedJoinPoolInput =
    { PoolId: Ulid
      PoolName: NonEmptyString100
      GamblerId: Ulid
      GamblerUsername: NonEmptyString100
      PoolLayoutId: Ulid }
