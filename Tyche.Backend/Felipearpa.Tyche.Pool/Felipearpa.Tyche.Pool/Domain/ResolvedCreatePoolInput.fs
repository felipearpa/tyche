namespace Felipearpa.Tyche.Pool.Domain

open Felipearpa.Type

type ResolvedCreatePoolInput =
    { PoolId: Ulid
      PoolName: NonEmptyString100
      PoolLayoutId: Ulid
      OwnerGamblerId: Ulid
      OwnerGamblerUsername: NonEmptyString100 }
