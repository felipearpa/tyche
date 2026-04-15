namespace Felipearpa.Tyche.Pool.Domain

open Felipearpa.Type

type PoolGamblerScore =
    { PoolId: Ulid
      PoolName: NonEmptyString100
      GamblerId: Ulid
      GamblerUsername: NonEmptyString100
      Position: int option
      BeforePosition: int option
      Score: int option
      BeforeScore: int option }
