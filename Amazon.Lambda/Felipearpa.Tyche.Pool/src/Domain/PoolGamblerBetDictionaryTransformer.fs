namespace Felipearpa.Tyche.Pool.Domain

open System
open System.Collections.Generic
open System.Runtime.CompilerServices
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb.Dictionary
open Felipearpa.Tyche.Pool.Infrastructure
open Felipearpa.Tyche.Pool.Type
open Felipearpa.Type

module PoolGamblerBetDictionaryTransformer =

    let toPoolGamblerBet (dictionary: IDictionary<string, AttributeValue>) =
        let matchDateTime =
            DateTime.Parse(
                dictionary[PoolTable.Attribute.matchDateTime].S,
                Globalization.CultureInfo.InvariantCulture,
                Globalization.DateTimeStyles.RoundtripKind
            )

        { PoolGamblerBet.PoolId = dictionary[PoolTable.Attribute.poolId].S |> Ulid.newOf
          GamblerId = dictionary[PoolTable.Attribute.gamblerId].S |> Ulid.newOf
          GamblerUsername = dictionary[PoolTable.Attribute.gamblerUsername].S |> NonEmptyString100.newOf
          MatchId = dictionary[PoolTable.Attribute.matchId].S |> Ulid.newOf
          PoolLayoutId = dictionary[PoolTable.Attribute.poolLayoutId].S |> Ulid.newOf
          HomeTeamId = dictionary[PoolTable.Attribute.homeTeamId].S |> Ulid.newOf
          HomeTeamName = dictionary[PoolTable.Attribute.homeTeamName].S |> NonEmptyString100.newOf
          AwayTeamId = dictionary[PoolTable.Attribute.awayTeamId].S |> Ulid.newOf
          AwayTeamName = dictionary[PoolTable.Attribute.awayTeamName].S |> NonEmptyString100.newOf
          MatchScore =
            match
                tryGetAttributeValueOrNone (PoolTable.Attribute.homeTeamScore) dictionary,
                tryGetAttributeValueOrNone (PoolTable.Attribute.awayTeamScore) dictionary
            with
            | Some homeTeamScore, Some awayTeamScore ->
                Some
                    { TeamScore.HomeTeamValue = int homeTeamScore.N
                      AwayTeamValue = int awayTeamScore.N }
            | _ -> None
          BetScore =
            match
                tryGetAttributeValueOrNone (PoolTable.Attribute.homeTeamBet) dictionary,
                tryGetAttributeValueOrNone (PoolTable.Attribute.awayTeamBet) dictionary
            with
            | Some homeTeamBet, Some awayTeamBet ->
                Some
                    { TeamScore.HomeTeamValue = homeTeamBet.N |> int |> BetScore.newOf
                      AwayTeamValue = awayTeamBet.N |> int |> BetScore.newOf }
            | _ -> None
          Score =
            dictionary
            |> tryGetAttributeValueOrNone (PoolTable.Attribute.score)
            |> Option.map (fun attributeValue -> int attributeValue.N)
          MatchDateTime = matchDateTime
          isLocked = LockPolicy.isLockedAt DateTime.UtcNow matchDateTime
          isComputed =
            dictionary
            |> tryGetAttributeValueOrNone (PoolTable.Attribute.computedRequestId)
            |> Option.isSome }

    type Extensions =
        [<Extension>]
        static member ToPoolGamblerBet(this: IDictionary<string, AttributeValue>) = toPoolGamblerBet this
