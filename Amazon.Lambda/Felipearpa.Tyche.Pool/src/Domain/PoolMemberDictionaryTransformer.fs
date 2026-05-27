namespace Felipearpa.Tyche.Pool.Domain

open System.Collections.Generic
open System.Runtime.CompilerServices
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb.Dictionary
open Felipearpa.Tyche.Pool.Infrastructure
open Felipearpa.Type

module PoolMemberDictionaryTransformer =

    let toPoolMember (dictionary: IDictionary<string, AttributeValue>) =
        { PoolMember.GamblerId = dictionary[PoolTable.Attribute.gamblerId].S |> Ulid.newOf
          GamblerUsername = dictionary[PoolTable.Attribute.gamblerUsername].S |> NonEmptyString100.newOf
          GamblerEmail =
            dictionary
            |> tryGetAttributeValueOrNone PoolTable.Attribute.gamblerEmail
            |> Option.map (fun attributeValue -> attributeValue.S)
            |> Option.defaultValue ""
          // Ownership is resolved by the use case, which knows the pool's creator.
          IsOwner = false }

    type Extensions =
        [<Extension>]
        static member ToPoolMember(this: IDictionary<string, AttributeValue>) = toPoolMember this
