namespace Felipearpa.Tyche.Pool.Domain

open Felipearpa.Tyche.Pool.Data
open Felipearpa.Type

module PoolTransformer =

    let toPool (poolEntity: PoolEntity) =
        { CreatePoolOutput.PoolId = poolEntity.PoolId |> Ulid.newOf
          PoolName = poolEntity.PoolName |> NonEmptyString100.newOf }
