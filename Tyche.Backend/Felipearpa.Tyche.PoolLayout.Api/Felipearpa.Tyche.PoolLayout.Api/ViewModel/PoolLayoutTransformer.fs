namespace Felipearpa.Tyche.PoolLayout.Api.ViewModel

open Felipearpa.Tyche.PoolLayout.Domain
open Felipearpa.Type

module PoolLayoutTransformer =
    let toPoolLayoutViewModel (poolLayout: PoolLayout) =
        { PoolLayoutViewModel.Id = poolLayout.Id |> Ulid.value
          Name = poolLayout.Name |> NonEmptyString100.value
          StartDateTime = poolLayout.StartDateTime }
