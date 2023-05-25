namespace Felipearpa.Tyche.PoolLayout.Domain

open System
open Felipearpa.Type

type PoolLayoutPK =
    { PoolLayoutId: Ulid }

type PoolLayout =
    { PoolLayoutPK: PoolLayoutPK
      Name: NonEmptyString100
      OpeningStartDateTime: DateTime
      OpeningEndDateTime: DateTime }
