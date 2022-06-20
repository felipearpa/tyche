namespace Pipel.Tyche.Pool.Domain

open Pipel.Tyche.Pool.Domain
open Pipel.Type

type PoolGamblerPK =
    { PoolPK: PoolPK
      GamblerPK: GamblerPK }

type PoolGambler =
    { PoolGamblerPK: PoolGamblerPK
      GamblerEmail: Email
      Score: PositiveInt option
      CurrentPosition: PositiveInt option
      BeforePosition: PositiveInt option }
