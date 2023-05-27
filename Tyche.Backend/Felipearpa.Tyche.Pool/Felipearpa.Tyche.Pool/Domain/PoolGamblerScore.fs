namespace Felipearpa.Tyche.Pool.Domain

open Felipearpa.Type

type PoolGamblerScore =
    { PoolId: Ulid
      PoolName: NonEmptyString100
      GamblerId: Ulid
      GamblerUsername: NonEmptyString100
      CurrentPosition: int option
      BeforePosition: int option
      Score: int option }
