namespace Pipel.Tyche.PoolLayout

open Pipel.Tyche.PoolLayout.Data
open Pipel.Tyche.PoolLayout.Domain
open Pipel.Type

module PoolLayoutDomainMapper =

    let mapFromDomainToData (persistenceModel: PoolLayoutEntity) =
        { PoolLayout.PoolLayoutPK = { PoolLayoutId = Uuid.From persistenceModel.PoolLayoutId }
          Name = NonEmptyString100.From persistenceModel.Name
          OpeningStartDateTime = DateTime.From persistenceModel.OpeningStartDateTime
          OpeningEndDateTime = DateTime.From persistenceModel.OpeningEndDateTime }
