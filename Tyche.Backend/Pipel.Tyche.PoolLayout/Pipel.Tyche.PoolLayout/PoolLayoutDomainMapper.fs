namespace Pipel.Tyche.PoolLayout

open Pipel.Tyche.PoolLayout.Data
open Pipel.Tyche.PoolLayout.Domain
open Pipel.Type

module PoolLayoutDomainMapper =

    let mapFromDomainToData (persistenceModel: PoolLayoutEntity) =
        { PoolLayout.PoolLayoutPK = { PoolLayoutId = Ulid.From persistenceModel.PoolLayoutId }
          Name = NonEmptyString100.From persistenceModel.Name
          OpeningStartDateTime = DateTime.From persistenceModel.StartOpeningDateTime
          OpeningEndDateTime = DateTime.From persistenceModel.EndOpeningDateTime }
