namespace Felipearpa.Tyche.PoolLayout.Api

open Felipearpa.Tyche.PoolLayout.Domain
open Felipearpa.Type

module PoolLayoutApplicationMapper =

    let mapFromDomainToApplication (domainModel: PoolLayout) =
        { PoolLayoutResponse.PoolLayoutId =
            domainModel.PoolLayoutPK.PoolLayoutId
            |> Ulid.value
          Name = domainModel.Name |> NonEmptyString100.value
          StartOpeningDateTime = domainModel.OpeningStartDateTime
          EndOpeningDateTime = domainModel.OpeningEndDateTime }
