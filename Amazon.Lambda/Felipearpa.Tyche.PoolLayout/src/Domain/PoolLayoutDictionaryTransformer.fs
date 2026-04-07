namespace Felipearpa.Tyche.PoolLayout.Domain

open System
open System.Collections.Generic
open System.Runtime.CompilerServices
open Amazon.DynamoDBv2.Model
open Felipearpa.Tyche.PoolLayout.Infrastructure
open Felipearpa.Type

module PoolLayoutDictionaryTransformer =
    let toPoolLayout (dictionary: IDictionary<string, AttributeValue>) =
        { PoolLayout.Id = dictionary[PoolLayoutTable.Attribute.poolLayoutId].S |> Ulid.newOf
          Name =
            dictionary[PoolLayoutTable.Attribute.poolLayoutName].S
            |> NonEmptyString100.newOf
          StartDateTime = DateTime.Parse(dictionary[PoolLayoutTable.Attribute.startDateTime].S) }

    type Extensions =
        [<Extension>]
        static member ToPoolLayout(this: IDictionary<string, AttributeValue>) = toPoolLayout this
