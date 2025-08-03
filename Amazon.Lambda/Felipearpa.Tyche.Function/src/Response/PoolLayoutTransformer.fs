namespace Felipearpa.Tyche.Function.Response

open System.Runtime.CompilerServices
open Felipearpa.Tyche.PoolLayout.Domain
open Felipearpa.Type

module PoolLayoutTransformer =
    let toPoolLayoutResponse (poolLayout: PoolLayout) =
        { PoolLayoutResponse.Id = poolLayout.Id |> Ulid.value
          Name = poolLayout.Name |> NonEmptyString100.value
          StartDateTime = poolLayout.StartDateTime }

    type Extensions =
        [<Extension>]
        static member ToPoolLayoutResponse(this: PoolLayout) = toPoolLayoutResponse this
