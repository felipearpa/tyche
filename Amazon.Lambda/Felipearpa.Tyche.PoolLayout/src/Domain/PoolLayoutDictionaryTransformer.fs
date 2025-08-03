namespace Felipearpa.Tyche.PoolLayout.Domain

open System
open System.Collections.Generic
open System.Runtime.CompilerServices
open Amazon.DynamoDBv2.Model
open Felipearpa.Type

module PoolLayoutDictionaryTransformer =
    let toPoolLayout (dictionary: IDictionary<string, AttributeValue>) =
        { PoolLayout.Id = dictionary["id"].S |> Ulid.newOf
          Name = dictionary["name"].S |> NonEmptyString100.newOf
          StartDateTime = dictionary["startDateTime"].S |> DateTime.Parse }

    type Extensions =
        [<Extension>]
        static member ToPoolLayout(this: IDictionary<string, AttributeValue>) = toPoolLayout this
