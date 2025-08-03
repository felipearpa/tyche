namespace Felipearpa.Tyche.Pool.Domain

open Felipearpa.Type

type CreatePoolInput =
    { PoolId: Ulid
      PoolName: NonEmptyString100
      PoolLayoutId: Ulid
      OwnerGamblerId: Ulid }
