namespace Felipearpa.Tyche.Pool.Test.Domain

open Amazon.DynamoDBv2.Model
open Felipearpa.Tyche.Pool.Domain.PoolGamblerBetDictionaryTransformer
open FsUnitTyped
open Xunit

module PoolGamblerBetDictionaryTransformerTest =

    [<Fact>]
    let ``given a bet dictionary when is transformed to a bet then the bet is returned`` () =
        let dictionary =
            dict
                [ "pk", AttributeValue(S = "POOL#01K1PX1TX2NM1HG851S1V0QG6N")
                  "sk", AttributeValue(S = "GAMBLER#01K1PX1TX2NM1HG851S1V0QG6N")
                  "poolId", AttributeValue(S = "01K1PX1TX2NM1HG851S1V0QG6N")
                  "gamblerId", AttributeValue(S = "01K1PX1TX2NM1HG851S1V0QG6N")
                  "matchId", AttributeValue(S = "01K1PX1TX2NM1HG851S1V0QG6N")
                  "poolLayoutId", AttributeValue(S = "01K1PX1TX2NM1HG851S1V0QG6N")
                  "homeTeamId", AttributeValue(S = "01K1PX1TX2NM1HG851S1V0QG6N")
                  "homeTeamName", AttributeValue(S = "Team Alpha")
                  "homeTeamScore", AttributeValue(N = "2")
                  "homeTeamBet", AttributeValue(N = "1")
                  "awayTeamId", AttributeValue(S = "01K1PX1TX2NM1HG851S1V0QG6N")
                  "awayTeamName", AttributeValue(S = "Team Beta")
                  "awayTeamScore", AttributeValue(N = "1")
                  "awayTeamBet", AttributeValue(N = "1")
                  "score", AttributeValue(N = "3")
                  "matchDateTime", AttributeValue(S = "2024-10-12T18:00:00Z") ]

        let bet = dictionary |> toPoolGamblerBet

        bet.PoolId.Value |> shouldEqual "01K1PX1TX2NM1HG851S1V0QG6N"
        bet.GamblerId.Value |> shouldEqual "01K1PX1TX2NM1HG851S1V0QG6N"
        bet.MatchId.Value |> shouldEqual "01K1PX1TX2NM1HG851S1V0QG6N"
        bet.PoolLayoutId.Value |> shouldEqual "01K1PX1TX2NM1HG851S1V0QG6N"
        bet.HomeTeamId.Value |> shouldEqual "01K1PX1TX2NM1HG851S1V0QG6N"
        bet.HomeTeamName.Value |> shouldEqual "Team Alpha"
        bet.MatchScore.Value.HomeTeamValue |> shouldEqual 2
        bet.MatchScore.Value.AwayTeamValue |> shouldEqual 1
        bet.AwayTeamId.Value |> shouldEqual "01K1PX1TX2NM1HG851S1V0QG6N"
        bet.AwayTeamName.Value |> shouldEqual "Team Beta"
        bet.BetScore.Value.HomeTeamValue.Value |> shouldEqual 1
        bet.BetScore.Value.AwayTeamValue.Value |> shouldEqual 1
        bet.Score.Value |> shouldEqual 3
        bet.MatchDateTime |> shouldEqual (System.DateTime.Parse("2024-10-12T18:00:00Z"))

    [<Fact>]
    let ``given a bet dictionary with the minimal properties when is transformed to a bet then the bet is returned``
        ()
        =
        let dictionary =
            dict
                [ "pk", AttributeValue(S = "POOL#01K1PX1TX2NM1HG851S1V0QG6N")
                  "sk", AttributeValue(S = "GAMBLER#01K1PX1TX2NM1HG851S1V0QG6N")
                  "poolId", AttributeValue(S = "01K1PX1TX2NM1HG851S1V0QG6N")
                  "gamblerId", AttributeValue(S = "01K1PX1TX2NM1HG851S1V0QG6N")
                  "matchId", AttributeValue(S = "01K1PX1TX2NM1HG851S1V0QG6N")
                  "poolLayoutId", AttributeValue(S = "01K1PX1TX2NM1HG851S1V0QG6N")
                  "homeTeamId", AttributeValue(S = "01K1PX1TX2NM1HG851S1V0QG6N")
                  "homeTeamName", AttributeValue(S = "Team Alpha")
                  "awayTeamId", AttributeValue(S = "01K1PX1TX2NM1HG851S1V0QG6N")
                  "awayTeamName", AttributeValue(S = "Team Beta")
                  "matchDateTime", AttributeValue(S = "2024-10-12T18:00:00Z") ]

        let bet = dictionary |> toPoolGamblerBet

        bet.PoolId.Value |> shouldEqual "01K1PX1TX2NM1HG851S1V0QG6N"
        bet.GamblerId.Value |> shouldEqual "01K1PX1TX2NM1HG851S1V0QG6N"
        bet.MatchId.Value |> shouldEqual "01K1PX1TX2NM1HG851S1V0QG6N"
        bet.PoolLayoutId.Value |> shouldEqual "01K1PX1TX2NM1HG851S1V0QG6N"
        bet.HomeTeamId.Value |> shouldEqual "01K1PX1TX2NM1HG851S1V0QG6N"
        bet.HomeTeamName.Value |> shouldEqual "Team Alpha"
        bet.MatchScore |> shouldEqual None
        bet.AwayTeamId.Value |> shouldEqual "01K1PX1TX2NM1HG851S1V0QG6N"
        bet.AwayTeamName.Value |> shouldEqual "Team Beta"
        bet.BetScore |> shouldEqual None
        bet.Score |> shouldEqual None
        bet.MatchDateTime |> shouldEqual (System.DateTime.Parse("2024-10-12T18:00:00Z"))
