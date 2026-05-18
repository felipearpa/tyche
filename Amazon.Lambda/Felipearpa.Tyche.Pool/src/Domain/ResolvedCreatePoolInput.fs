namespace Felipearpa.Tyche.Pool.Domain

open Felipearpa.Type

type ResolvedCreatePoolInput =
    { PoolId: Ulid
      PoolName: NonEmptyString100
      PoolLayoutId: Ulid
      PoolLayoutVersion: int
      OwnerGamblerId: Ulid
      OwnerGamblerUsername: NonEmptyString100
      OwnerGamblerEmail: NonEmptyString100 }
