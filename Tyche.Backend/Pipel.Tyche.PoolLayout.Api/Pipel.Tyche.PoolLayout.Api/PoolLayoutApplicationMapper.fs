namespace Pipel.Tyche.PoolLayout.Api

open Pipel.Tyche.PoolLayout.Domain
open Pipel.Type

module PoolLayoutApplicationMapper =

    let mapFromDomainToApplication (domainModel: PoolLayout) =
        { PoolLayoutResponse.PoolLayoutId =
              domainModel.PoolLayoutPK.PoolLayoutId
              |> Ulid.toString
          Name = domainModel.Name |> NonEmptyString100.value
          StartOpeningDateTime = domainModel.OpeningStartDateTime |> DateTime.value
          EndOpeningDateTime = domainModel.OpeningEndDateTime |> DateTime.value }
