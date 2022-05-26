namespace Pipel.Tyche.Pool.Api

open System

type PoolResponse =
    { PoolId: string
      PoolLayoutId: string
      PoolName: string
      CurrentPosition: int Nullable
      BeforePosition: int Nullable }
