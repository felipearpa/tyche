namespace Felipearpa.Tyche.Pool.Domain

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.Pool.Infrastructure

module InitialPoolGamblerBetTransformer =

    let toAmazonItem (poolGamblerBet: InitialPoolGamblerBet) : IDictionary<string, AttributeValue> =
        let matchDateTime = poolGamblerBet.MatchDateTime.ToUniversalTime().ToString("o")

        let item =
            Dictionary<string, AttributeValue>(
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
                      PoolTable.Attribute.poolLayoutVersion,
                      AttributeValue(N = poolGamblerBet.PoolLayoutVersion.ToString())
                      PoolTable.Attribute.getPoolGamblerScoresByMatchPk,
                      AttributeValue(S = KeyPrefix.build PoolTable.Prefix.match' poolGamblerBet.MatchId.Value)
                      PoolTable.Attribute.getPoolGamblerScoresByMatchSk,
                      AttributeValue(
                          S =
                              $"{KeyPrefix.build PoolTable.Prefix.pool poolGamblerBet.PoolId.Value}#{KeyPrefix.build PoolTable.Prefix.gambler poolGamblerBet.GamblerId.Value}"
                      ) ]
            )

        match poolGamblerBet.HomeTeamScore with
        | Some homeTeamScore -> item[PoolTable.Attribute.homeTeamScore] <- AttributeValue(N = homeTeamScore.ToString())
        | None -> ()

        match poolGamblerBet.AwayTeamScore with
        | Some awayTeamScore -> item[PoolTable.Attribute.awayTeamScore] <- AttributeValue(N = awayTeamScore.ToString())
        | None -> ()

        match poolGamblerBet.BetScore with
        | Some betScore -> item[PoolTable.Attribute.score] <- AttributeValue(N = betScore.ToString())
        | None -> ()

        match poolGamblerBet.ComputedDateTime with
        | Some computedDateTime ->
            item[PoolTable.Attribute.computedDateTime] <-
                AttributeValue(S = computedDateTime.ToUniversalTime().ToString("o"))
        | None -> ()

        match poolGamblerBet.ComputedRequestId with
        | Some computedRequestId ->
            item[PoolTable.Attribute.computedRequestId] <- AttributeValue(S = computedRequestId)
        | None -> ()

        item :> IDictionary<string, AttributeValue>
