namespace Felipearpa.Tyche.Pool.Domain

open Felipearpa.Type

type PoolMember =
    { GamblerId: Ulid
      GamblerUsername: NonEmptyString100
      GamblerEmail: string }
