namespace Felipearpa.Tyche.Function.Response

open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

module PoolTransformer =
    let toPoolViewModel (pool: Pool) =
        { PoolResponse.PoolId = pool.PoolId |> Ulid.value
          PoolName = pool.PoolName |> NonEmptyString100.value }
