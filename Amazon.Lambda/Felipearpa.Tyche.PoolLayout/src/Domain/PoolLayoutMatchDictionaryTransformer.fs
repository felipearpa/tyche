namespace Felipearpa.Tyche.PoolLayout.Domain

open System
open System.Collections.Generic
open System.Runtime.CompilerServices
open Amazon.DynamoDBv2.Model
open Felipearpa.Tyche.PoolLayout.Infrastructure
open Felipearpa.Type

module PoolLayoutMatchDictionaryTransformer =
    [<Literal>]
    let private defaultRound = "Fase de grupos"

    let private tryGetIntAttribute (dictionary: IDictionary<string, AttributeValue>) (key: string) =
        match dictionary.TryGetValue(key) with
        | true, value -> value.N |> int |> Some
        | false, _ -> None

    let private getStringOrDefault (dictionary: IDictionary<string, AttributeValue>) (key: string) (defaultValue: string) =
        match dictionary.TryGetValue(key) with
        | true, value -> value.S
        | false, _ -> defaultValue

    let toPoolLayoutMatch (dictionary: IDictionary<string, AttributeValue>) =
        { PoolLayoutMatch.MatchId = dictionary[PoolLayoutTable.Attribute.matchId].S |> Ulid.newOf
          PoolLayoutId = dictionary[PoolLayoutTable.Attribute.poolLayoutId].S |> Ulid.newOf
          HomeTeamId = dictionary[PoolLayoutTable.Attribute.homeTeamId].S
          HomeTeamName = dictionary[PoolLayoutTable.Attribute.homeTeamName].S |> NonEmptyString100.newOf
          AwayTeamId = dictionary[PoolLayoutTable.Attribute.awayTeamId].S
          AwayTeamName = dictionary[PoolLayoutTable.Attribute.awayTeamName].S |> NonEmptyString100.newOf
          MatchDateTime = DateTime.Parse(dictionary[PoolLayoutTable.Attribute.matchDateTime].S)
          PoolLayoutVersion = dictionary[PoolLayoutTable.Attribute.poolLayoutVersion].N |> int
          Round = getStringOrDefault dictionary PoolLayoutTable.Attribute.round defaultRound
          HomeTeamScore = tryGetIntAttribute dictionary PoolLayoutTable.Attribute.homeTeamScore
          AwayTeamScore = tryGetIntAttribute dictionary PoolLayoutTable.Attribute.awayTeamScore }

    type Extensions =
        [<Extension>]
        static member ToPoolLayoutMatch(this: IDictionary<string, AttributeValue>) = toPoolLayoutMatch this
