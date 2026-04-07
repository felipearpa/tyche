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
          CurrentPosition =
            dictionary
            |> tryGetAttributeValueOrNone (PoolTable.Attribute.currentPosition)
            |> noneIfZero
          BeforePosition =
            dictionary
            |> tryGetAttributeValueOrNone (PoolTable.Attribute.beforePosition)
            |> noneIfZero
          Score =
            dictionary
            |> tryGetAttributeValueOrNone (PoolTable.Attribute.score)
            |> noneIfZero }

    type Extensions =
        [<Extension>]
        static member ToPoolGamblerScore(this: IDictionary<string, AttributeValue>) = toPoolGamblerScore this
