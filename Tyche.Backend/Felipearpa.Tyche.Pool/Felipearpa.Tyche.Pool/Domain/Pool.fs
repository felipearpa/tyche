namespace Felipearpa.Tyche.Pool.Domain

open Felipearpa.Type

type Pool =
    { PoolId: Ulid
      PoolName: NonEmptyString100 }
