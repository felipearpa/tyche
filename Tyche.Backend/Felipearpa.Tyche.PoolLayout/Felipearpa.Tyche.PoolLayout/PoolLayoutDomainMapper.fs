namespace Felipearpa.Tyche.PoolLayout

open Felipearpa.Tyche.PoolLayout.Data
open Felipearpa.Tyche.PoolLayout.Domain
open Felipearpa.Type

module PoolLayoutDomainMapper =

    let mapFromDomainToData (persistenceModel: PoolLayoutEntity) =
        { PoolLayout.PoolLayoutPK = { PoolLayoutId = Ulid.newOf persistenceModel.PoolLayoutId }
          Name = NonEmptyString100.newOf persistenceModel.Name
          OpeningStartDateTime = persistenceModel.StartOpeningDateTime
          OpeningEndDateTime = persistenceModel.EndOpeningDateTime }
