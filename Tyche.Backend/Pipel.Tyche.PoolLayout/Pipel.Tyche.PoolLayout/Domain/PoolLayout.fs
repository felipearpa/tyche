namespace Pipel.Tyche.PoolLayout.Domain

open Pipel.Type

type PoolLayoutPK =
    { PoolLayoutId: Uuid }

type PoolLayout =
    { PoolLayoutPK: PoolLayoutPK
      Name: NonEmptyString100
      OpeningStartDateTime: DateTime
      OpeningEndDateTime: DateTime }
