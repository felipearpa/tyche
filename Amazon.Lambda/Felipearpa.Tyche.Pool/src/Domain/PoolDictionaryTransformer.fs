namespace Felipearpa.Tyche.Pool.Domain

open System
open System.Collections.Generic
open System.Globalization
open System.Runtime.CompilerServices
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb.Dictionary
open Felipearpa.Tyche.Pool.Infrastructure
open Felipearpa.Type

module PoolDictionaryTransformer =
    let toPool (dictionary: IDictionary<string, AttributeValue>) =
        { Pool.PoolId = dictionary[PoolTable.Attribute.poolId].S |> Ulid.newOf
          PoolName = dictionary[PoolTable.Attribute.poolName].S |> NonEmptyString100.newOf
          PoolLayoutId = dictionary[PoolTable.Attribute.poolLayoutId].S |> Ulid.newOf
          CreatorGamblerId = dictionary[PoolTable.Attribute.creatorGamblerId].S |> Ulid.newOf
          GamblerCount =
            dictionary
            |> tryGetAttributeValueOrNone PoolTable.Attribute.gamblerCount
            |> Option.map (fun attributeValue -> int attributeValue.N) }

    let toGamblerCountEntry (dictionary: IDictionary<string, AttributeValue>) =
        dictionary
        |> tryGetAttributeValueOrNone PoolTable.Attribute.gamblerCount
        |> Option.map (fun gamblerCount -> dictionary[PoolTable.Attribute.poolId].S |> Ulid.newOf, int gamblerCount.N)

    let toPoolTrend (dictionary: IDictionary<string, AttributeValue>) =
        { PoolTrend.PoolId = dictionary[PoolTable.Attribute.poolId].S |> Ulid.newOf
          PoolLayoutId = dictionary[PoolTable.Attribute.poolLayoutId].S |> Ulid.newOf
          TrendKickoffDateTime =
            dictionary
            |> tryGetAttributeValueOrNone PoolTable.Attribute.trendKickoffDateTime
            |> Option.map (fun value ->
                DateTime.Parse(value.S, CultureInfo.InvariantCulture, DateTimeStyles.RoundtripKind)) }

    type Extensions =
        [<Extension>]
        static member ToPool(this: IDictionary<string, AttributeValue>) = toPool this

        [<Extension>]
        static member ToGamblerCountEntry(this: IDictionary<string, AttributeValue>) = toGamblerCountEntry this
