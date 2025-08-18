namespace Felipearpa.Tyche.Pool.Domain

open System
open System.Collections.Generic
open System.Runtime.CompilerServices
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb.Dictionary
open Felipearpa.Tyche.Pool.Type
open Felipearpa.Type

module PoolGamblerBetDictionaryTransformer =

    let toPoolGamblerBet (dictionary: IDictionary<string, AttributeValue>) =
        let matchDateTime = DateTime.Parse(dictionary["matchDateTime"].S)

        { PoolGamblerBet.PoolId = dictionary["poolId"].S |> Ulid.newOf
          GamblerId = dictionary["gamblerId"].S |> Ulid.newOf
          MatchId = dictionary["matchId"].S |> Ulid.newOf
          PoolLayoutId = dictionary["poolLayoutId"].S |> Ulid.newOf
          HomeTeamId = dictionary["homeTeamId"].S |> Ulid.newOf
          HomeTeamName = dictionary["homeTeamName"].S |> NonEmptyString100.newOf
          AwayTeamId = dictionary["awayTeamId"].S |> Ulid.newOf
          AwayTeamName = dictionary["awayTeamName"].S |> NonEmptyString100.newOf
          MatchScore =
            match
                tryGetAttributeValueOrNone "homeTeamScore" dictionary,
                tryGetAttributeValueOrNone "awayTeamScore" dictionary
            with
            | Some homeTeamScore, Some awayTeamScore ->
                Some
                    { TeamScore.HomeTeamValue = int homeTeamScore.N
                      AwayTeamValue = int awayTeamScore.N }
            | _ -> None
          BetScore =
            match
                tryGetAttributeValueOrNone "homeTeamBet" dictionary, tryGetAttributeValueOrNone "awayTeamBet" dictionary
            with
            | Some homeTeamBet, Some awayTeamBet ->
                Some
                    { TeamScore.HomeTeamValue = homeTeamBet.N |> int |> BetScore.newOf
                      AwayTeamValue = awayTeamBet.N |> int |> BetScore.newOf }
            | _ -> None
          Score = tryGetAttributeValueOrNone "score" dictionary |> Option.map (fun it -> int it.N)
          MatchDateTime = matchDateTime
          isLocked = DateTime.Now >= matchDateTime }

    type Extensions =
        [<Extension>]
        static member ToPoolGamblerBet(this: IDictionary<string, AttributeValue>) = toPoolGamblerBet this
