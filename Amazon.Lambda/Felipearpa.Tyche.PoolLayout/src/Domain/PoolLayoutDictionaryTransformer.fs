namespace Felipearpa.Tyche.PoolLayout.Domain

open System
open System.Collections.Generic
open System.Runtime.CompilerServices
open Amazon.DynamoDBv2.Model
open Felipearpa.Type

module PoolLayoutDictionaryTransformer =
    let toPoolLayout (dictionary: IDictionary<string, AttributeValue>) =
        { PoolLayout.Id = dictionary["poolLayoutId"].S |> Ulid.newOf
          Name = dictionary["poolLayoutName"].S |> NonEmptyString100.newOf
          StartDateTime = DateTime.Parse(dictionary["startDateTime"].S) }

    type Extensions =
        [<Extension>]
        static member ToPoolLayout(this: IDictionary<string, AttributeValue>) = toPoolLayout this
