namespace Felipearpa.Tyche.Pool.Domain

open Felipearpa.Type

type PoolLayoutGambler =
    { PoolId: Ulid
      PoolLayoutId: Ulid
      PoolLayoutVersion: int
      GamblerId: Ulid
      GamblerUsername: NonEmptyString100
      GamblerEmail: NonEmptyString100 }
