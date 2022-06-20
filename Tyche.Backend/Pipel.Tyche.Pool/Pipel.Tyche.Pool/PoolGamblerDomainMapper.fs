namespace Pipel.Tyche.Pool

open Pipel.Tyche.Pool.Data
open Pipel.Tyche.Pool.Domain
open Pipel.Type

module PoolGamblerDomainMapper =

    let mapFromDomainToData (persistenceModel: PoolGamblerEntity) =
        { PoolGambler.PoolGamblerPK = { PoolPK = { PoolId = Ulid.From persistenceModel.GamblerId  }
                                        GamblerPK = { GamblerId = Ulid.From persistenceModel.GamblerId } }
          GamblerEmail = Email.From persistenceModel.GamblerEmail
          Score = PositiveInt.TryFromNullable persistenceModel.Score
          CurrentPosition = PositiveInt.TryFromNullable persistenceModel.CurrentPosition
          BeforePosition = PositiveInt.TryFromNullable persistenceModel.BeforePosition }
