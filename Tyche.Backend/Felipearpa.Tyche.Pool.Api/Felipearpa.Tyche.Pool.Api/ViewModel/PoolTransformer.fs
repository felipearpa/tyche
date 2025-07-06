namespace Felipearpa.Tyche.Pool.Api.ViewModel

open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

module PoolTransformer =
    let toPoolViewModel (pool: CreatePoolOutput) =
        { PoolViewModel.PoolId = pool.PoolId |> Ulid.value
          PoolName = pool.PoolName |> NonEmptyString100.value }
