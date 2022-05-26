namespace Pipel.Tyche.Pool

open Pipel.Tyche.Pool.Data
open Pipel.Tyche.Pool.Domain
open Pipel.Type

module PoolDomainMapper =

    let mapFromDomainToData (persistenceModel: PoolEntity) =
        { Pool.PoolLayoutPK = { PoolLayoutId = Ulid.From persistenceModel.PoolLayoutId }
          PoolPK = { PoolPK.PoolId = Ulid.From persistenceModel.PoolId }
          PoolName = NonEmptyString100.From persistenceModel.PoolName
          CurrentPosition = PositiveInt.TryFromNullable persistenceModel.CurrentPosition
          BeforePosition = PositiveInt.TryFromNullable persistenceModel.BeforePosition }
