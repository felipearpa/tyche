namespace Pipel.Tyche.PoolLayout.Domain

open Pipel.Type

type PoolLayoutPK =
    { PoolLayoutId: Ulid }

type PoolLayout =
    { PoolLayoutPK: PoolLayoutPK
      Name: NonEmptyString100
      OpeningStartDateTime: DateTime
      OpeningEndDateTime: DateTime }
