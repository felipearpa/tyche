namespace Felipearpa.Tyche.PoolLayout.Domain

open System
open System.Collections.Generic
open System.Runtime.CompilerServices
open Amazon.DynamoDBv2.Model
open Felipearpa.Tyche.PoolLayout.Infrastructure
open Felipearpa.Type

module PoolLayoutMatchDictionaryTransformer =
    let toPoolLayoutMatch (dictionary: IDictionary<string, AttributeValue>) =
        { PoolLayoutMatch.MatchId = dictionary[PoolLayoutTable.Attribute.matchId].S |> Ulid.newOf
          PoolLayoutId = dictionary[PoolLayoutTable.Attribute.poolLayoutId].S |> Ulid.newOf
          HomeTeamId = dictionary[PoolLayoutTable.Attribute.homeTeamId].S |> Ulid.newOf
          HomeTeamName = dictionary[PoolLayoutTable.Attribute.homeTeamName].S |> NonEmptyString100.newOf
          AwayTeamId = dictionary[PoolLayoutTable.Attribute.awayTeamId].S |> Ulid.newOf
          AwayTeamName = dictionary[PoolLayoutTable.Attribute.awayTeamName].S |> NonEmptyString100.newOf
          MatchDateTime = DateTime.Parse(dictionary[PoolLayoutTable.Attribute.matchDateTime].S)
          PoolLayoutVersion = dictionary[PoolLayoutTable.Attribute.poolLayoutVersion].N |> int }

    type Extensions =
        [<Extension>]
        static member ToPoolLayoutMatch(this: IDictionary<string, AttributeValue>) = toPoolLayoutMatch this
