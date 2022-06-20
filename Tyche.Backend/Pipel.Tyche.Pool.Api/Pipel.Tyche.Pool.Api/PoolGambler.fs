namespace Pipel.Tyche.Pool.Api

open System

type PoolGamblerResponse =
    { PoolId: string
      GamblerId: string
      GamblerEmail: string
      Score: int Nullable
      CurrentPosition: int Nullable
      BeforePosition: int Nullable }
