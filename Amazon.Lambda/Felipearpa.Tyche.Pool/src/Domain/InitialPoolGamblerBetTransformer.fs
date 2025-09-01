namespace Felipearpa.Tyche.Pool.Domain

open System.Collections.Generic
open Amazon.DynamoDBv2.Model

module InitialPoolGamblerBetTransformer =

    [<Literal>]
    let poolKeyPrefix = "POOL"

    [<Literal>]
    let gamblerKeyPrefix = "GAMBLER"

    [<Literal>]
    let matchKeyPrefix = "MATCH"

    let toAmazonItem (poolGamblerBet: InitialPoolGamblerBet) : IDictionary<string, AttributeValue> =
        let matchDateTime = poolGamblerBet.MatchDateTime.ToUniversalTime().ToString("o")

        dict
            [ "pk",
              AttributeValue(
                  S = $"{gamblerKeyPrefix}#{poolGamblerBet.GamblerId}#{poolKeyPrefix}#{poolGamblerBet.PoolId}"
              )
              "sk", AttributeValue(S = $"{matchKeyPrefix}#{poolGamblerBet.MatchId}")
              "poolId", AttributeValue(S = poolGamblerBet.PoolId.Value)
              "gamblerId", AttributeValue(S = poolGamblerBet.GamblerId.Value)
              "matchId", AttributeValue(S = poolGamblerBet.MatchId.Value)
              "homeTeamId", AttributeValue(S = poolGamblerBet.HomeTeamId.Value)
              "homeTeamName", AttributeValue(S = poolGamblerBet.HomeTeamName.Value)
              "awayTeamId", AttributeValue(S = poolGamblerBet.AwayTeamId.Value)
              "awayTeamName", AttributeValue(S = poolGamblerBet.AwayTeamName.Value)
              "matchDateTime", AttributeValue(S = matchDateTime)
              "poolLayoutId", AttributeValue(S = poolGamblerBet.PoolLayoutId.Value)
              "poolLayoutVersion", AttributeValue(N = poolGamblerBet.PoolLayoutVersion.ToString())
              "getPendingPoolGamblerBetsSk", AttributeValue(S = matchDateTime) ]
