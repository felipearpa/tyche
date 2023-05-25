namespace Felipearpa.Tyche.Pool.Api.ViewModel

open System

type PoolGamblerScoreViewModel =
    { PoolId: string
      PoolLayoutId: string
      PoolName: string
      GamblerId: string
      GamblerUsername: string
      CurrentPosition: int Nullable
      BeforePosition: int Nullable
      Score: int Nullable }
