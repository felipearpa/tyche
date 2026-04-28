namespace Felipearpa.Tyche.Pool.Domain

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.Pool.Infrastructure

module InitialPoolGamblerBetTransformer =

    let toAmazonItem (poolGamblerBet: InitialPoolGamblerBet) : IDictionary<string, AttributeValue> =
        let matchDateTime = poolGamblerBet.MatchDateTime.ToUniversalTime().ToString("o")

        dict
            [ Key.pk,
              AttributeValue(
                  S =
                      $"{KeyPrefix.build PoolTable.Prefix.gambler poolGamblerBet.GamblerId.Value}#{KeyPrefix.build PoolTable.Prefix.pool poolGamblerBet.PoolId.Value}"
              )
              Key.sk, AttributeValue(S = KeyPrefix.build PoolTable.Prefix.match' poolGamblerBet.MatchId.Value)
              PoolTable.Attribute.poolId, AttributeValue(S = poolGamblerBet.PoolId.Value)
              PoolTable.Attribute.gamblerId, AttributeValue(S = poolGamblerBet.GamblerId.Value)
              PoolTable.Attribute.gamblerUsername, AttributeValue(S = poolGamblerBet.GamblerUsername.Value)
              PoolTable.Attribute.matchId, AttributeValue(S = poolGamblerBet.MatchId.Value)
              PoolTable.Attribute.homeTeamId, AttributeValue(S = poolGamblerBet.HomeTeamId.Value)
              PoolTable.Attribute.homeTeamName, AttributeValue(S = poolGamblerBet.HomeTeamName.Value)
              PoolTable.Attribute.awayTeamId, AttributeValue(S = poolGamblerBet.AwayTeamId.Value)
              PoolTable.Attribute.awayTeamName, AttributeValue(S = poolGamblerBet.AwayTeamName.Value)
              PoolTable.Attribute.matchDateTime, AttributeValue(S = matchDateTime)
              PoolTable.Attribute.poolLayoutId, AttributeValue(S = poolGamblerBet.PoolLayoutId.Value)
              PoolTable.Attribute.poolLayoutVersion, AttributeValue(N = poolGamblerBet.PoolLayoutVersion.ToString())
              PoolTable.Attribute.getPoolGamblerScoresByMatchPk,
              AttributeValue(S = KeyPrefix.build PoolTable.Prefix.match' poolGamblerBet.MatchId.Value)
              PoolTable.Attribute.getPoolGamblerScoresByMatchSk,
              AttributeValue(
                  S =
                      $"{KeyPrefix.build PoolTable.Prefix.pool poolGamblerBet.PoolId.Value}#{KeyPrefix.build PoolTable.Prefix.gambler poolGamblerBet.GamblerId.Value}"
              ) ]
