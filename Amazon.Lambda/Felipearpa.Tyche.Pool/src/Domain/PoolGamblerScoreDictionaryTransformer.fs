namespace Felipearpa.Tyche.Pool.Domain

open System.Collections.Generic
open System.Runtime.CompilerServices
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb.Dictionary
open Felipearpa.Tyche.Pool.Infrastructure
open Felipearpa.Type

module PoolGamblerScoreDictionaryTransformer =

    let toPoolGamblerScore (dictionary: IDictionary<string, AttributeValue>) =
        { PoolGamblerScore.PoolId = dictionary[PoolTable.Attribute.poolId].S |> Ulid.newOf
          GamblerId = dictionary[PoolTable.Attribute.gamblerId].S |> Ulid.newOf
          PoolName = dictionary[PoolTable.Attribute.poolName].S |> NonEmptyString100.newOf
          GamblerUsername = dictionary[PoolTable.Attribute.gamblerUsername].S |> NonEmptyString100.newOf
          Position =
            dictionary
            |> tryGetAttributeValueOrNone (PoolTable.Attribute.position)
            |> noneIfZero
          BeforePosition =
            dictionary
            |> tryGetAttributeValueOrNone (PoolTable.Attribute.beforePosition)
            |> noneIfZero
          Score =
            dictionary
            |> tryGetAttributeValueOrNone (PoolTable.Attribute.score)
            |> noneIfZero
          BeforeScore =
            dictionary
            |> tryGetAttributeValueOrNone (PoolTable.Attribute.beforeScore)
            |> noneIfZero }

    type Extensions =
        [<Extension>]
        static member ToPoolGamblerScore(this: IDictionary<string, AttributeValue>) = toPoolGamblerScore this
