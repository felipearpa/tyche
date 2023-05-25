namespace Felipearpa.Tyche.Pool.Domain

open Felipearpa.Type

module PoolMapper =

    let mapToDomain (poolEntity: PoolEntity) =
        { Pool.PoolId = poolEntity.PoolId |> Ulid.newOf
          PoolName = poolEntity.PoolName |> NonEmptyString100.newOf }
