namespace Pipel.Tyche.Pool

open Pipel.Tyche.Pool.Data
open Pipel.Tyche.Pool.Domain
open Pipel.Type

module PoolGameDomainMapper =

    let mapFromDomainToData (persistenceModel: PoolGameEntity) =
        { PoolGame.PoolGamePK =
            { PoolGamePK.PoolPK = { PoolPK.PoolId = Ulid.From persistenceModel.PoolId }
              GamePK = { GamePK.GameId = Ulid.From persistenceModel.GameId } }
          HomeTeamPK = { TeamPK.TeamId = Ulid.From persistenceModel.HomeTeamId }
          HomeTeamName = NonEmptyString100.From persistenceModel.HomeTeamName
          HomeTeamScore = PositiveInt.TryFromNullable persistenceModel.HomeTeamScore
          HomeTeamBet = PositiveInt.TryFromNullable persistenceModel.HomeTeamBet
          AwayTeamPK = { TeamPK.TeamId = Ulid.From persistenceModel.AwayTeamId }
          AwayTeamName = NonEmptyString100.From persistenceModel.AwayTeamName
          AwayTeamScore = PositiveInt.TryFromNullable persistenceModel.AwayTeamScore
          AwayTeamBet = PositiveInt.TryFromNullable persistenceModel.AwayTeamBet
          BetScore = PositiveInt.TryFromNullable persistenceModel.BetScore
          MatchDateTime = DateTime.From persistenceModel.MatchDateTime }
