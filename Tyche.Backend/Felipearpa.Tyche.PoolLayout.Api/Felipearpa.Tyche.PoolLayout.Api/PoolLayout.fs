namespace Felipearpa.Tyche.PoolLayout.Api

open System

type PoolLayoutResponse =
    { PoolLayoutId: string
      Name: string
      StartOpeningDateTime: DateTime
      EndOpeningDateTime: DateTime }
