namespace Felipearpa.Tyche.PoolLayout.Domain

open System
open Felipearpa.Type

type PoolLayout =
    { Id: Ulid
      Name: NonEmptyString100
      StartDateTime: DateTime }
