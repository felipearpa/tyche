namespace Pipel.Tyche.Pool.Domain

open Pipel.Tyche.Pool.Domain
open Pipel.Type

type PoolPK = { PoolId: Ulid }

type Pool =
    { PoolPK: PoolPK
      PoolLayoutPK: PoolLayoutPK
      PoolName: NonEmptyString100
      CurrentPosition: PositiveInt option
      BeforePosition: PositiveInt option }
