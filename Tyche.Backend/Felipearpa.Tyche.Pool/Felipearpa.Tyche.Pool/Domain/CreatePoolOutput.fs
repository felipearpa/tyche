namespace Felipearpa.Tyche.Pool.Domain

open Felipearpa.Type

type CreatePoolOutput =
    { PoolId: Ulid
      PoolName: NonEmptyString100 }
