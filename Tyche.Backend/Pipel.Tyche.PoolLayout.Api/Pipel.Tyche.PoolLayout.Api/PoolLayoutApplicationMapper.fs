namespace Pipel.Tyche.PoolLayout.Api

open Pipel.Tyche.PoolLayout.Domain
open Pipel.Type

module PoolLayoutApplicationMapper =

    let mapFromDomainToApplication (domainModel: PoolLayout) =
        { PoolLayoutResponse.PoolLayoutId =
              domainModel.PoolLayoutPK.PoolLayoutId
              |> Uuid.value
          Name = domainModel.Name |> NonEmptyString100.value
          OpeningStartDateTime = domainModel.OpeningStartDateTime |> DateTime.value
          OpeningEndDateTime = domainModel.OpeningEndDateTime |> DateTime.value }
