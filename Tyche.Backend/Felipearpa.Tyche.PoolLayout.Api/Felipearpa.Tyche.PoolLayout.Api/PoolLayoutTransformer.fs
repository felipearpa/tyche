namespace Felipearpa.Tyche.PoolLayout.Api

open Felipearpa.Tyche.PoolLayout.Domain
open Felipearpa.Type

module PoolLayoutTransformer =
    let toPoolLayoutResponse (poolLayout: PoolLayout) =
        { PoolLayoutResponse.Id = poolLayout.Id |> Ulid.value
          Name = poolLayout.Name |> NonEmptyString100.value
          StartDateTime = poolLayout.StartDateTime }
