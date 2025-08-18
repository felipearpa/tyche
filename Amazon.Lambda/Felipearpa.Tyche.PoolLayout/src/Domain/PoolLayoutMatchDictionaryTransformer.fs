namespace Felipearpa.Tyche.PoolLayout.Domain

open System
open System.Collections.Generic
open System.Runtime.CompilerServices
open Amazon.DynamoDBv2.Model
open Felipearpa.Type

module PoolLayoutMatchDictionaryTransformer =
    let toPoolLayoutMatch (dictionary: IDictionary<string, AttributeValue>) =
        { PoolLayoutMatch.MatchId = dictionary["matchId"].S |> Ulid.newOf
          PoolLayoutId = dictionary["poolLayoutId"].S |> Ulid.newOf
          HomeTeamId = dictionary["homeTeamId"].S |> Ulid.newOf
          HomeTeamName = dictionary["homeTeamName"].S |> NonEmptyString100.newOf
          AwayTeamId = dictionary["awayTeamId"].S |> Ulid.newOf
          AwayTeamName = dictionary["awayTeamName"].S |> NonEmptyString100.newOf
          MatchDateTime = DateTime.Parse(dictionary["matchDateTime"].S)
          PoolLayoutVersion = dictionary["poolLayoutVersion"].N |> int }

    type Extensions =
        [<Extension>]
        static member ToPoolLayoutMatch(this: IDictionary<string, AttributeValue>) = toPoolLayoutMatch this
