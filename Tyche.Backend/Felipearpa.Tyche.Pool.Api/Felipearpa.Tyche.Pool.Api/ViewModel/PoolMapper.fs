namespace Felipearpa.Tyche.Pool.Api.ViewModel

open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

module PoolMapper =

    let mapToViewModel (pool: Pool) =
        { PoolViewModel.PoolId = pool.PoolId |> Ulid.value
          PoolName = pool.PoolName |> NonEmptyString100.value }
