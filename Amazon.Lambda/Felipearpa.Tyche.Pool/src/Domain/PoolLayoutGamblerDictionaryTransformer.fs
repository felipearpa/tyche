namespace Felipearpa.Tyche.Pool.Domain

open System.Collections.Generic
open System.Runtime.CompilerServices
open Amazon.DynamoDBv2.Model
open Felipearpa.Tyche.Pool.Infrastructure
open Felipearpa.Type

module PoolLayoutGamblerDictionaryTransformer =

    let toPoolLayoutGambler (dictionary: IDictionary<string, AttributeValue>) =
        { PoolLayoutGambler.PoolId = dictionary[PoolTable.Attribute.poolId].S |> Ulid.newOf
          PoolLayoutId = dictionary[PoolTable.Attribute.poolLayoutId].S |> Ulid.newOf
          PoolLayoutVersion = dictionary[PoolTable.Attribute.poolLayoutVersion].N |> int
          GamblerId = dictionary[PoolTable.Attribute.gamblerId].S |> Ulid.newOf
          GamblerUsername = dictionary[PoolTable.Attribute.gamblerUsername].S |> NonEmptyString100.newOf }

    type Extensions =
        [<Extension>]
        static member ToPoolLayoutGambler(this: IDictionary<string, AttributeValue>) = toPoolLayoutGambler this
