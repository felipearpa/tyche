namespace Pipel.Tyche.PoolLayout.Api

open System

type PoolLayoutResponse =
    { PoolLayoutId: Guid
      Name: string
      OpeningStartDateTime: DateTime
      OpeningEndDateTime: DateTime }
