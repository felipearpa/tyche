namespace Felipearpa.Tyche.Pool.Domain

open System.Collections.Generic
open System.Runtime.CompilerServices
open Amazon.DynamoDBv2.Model
open Felipearpa.Data.DynamoDb.Dictionary
open Felipearpa.Type

module PoolGamblerScoreDictionaryTransformer =

    let toPoolGamblerScore (dictionary: IDictionary<string, AttributeValue>) =
        { PoolGamblerScore.PoolId = dictionary["poolId"].S |> Ulid.newOf
          GamblerId = dictionary["gamblerId"].S |> Ulid.newOf
          PoolName = dictionary["poolName"].S |> NonEmptyString100.newOf
          GamblerUsername = dictionary["gamblerUsername"].S |> NonEmptyString100.newOf
          CurrentPosition = dictionary |> tryGetAttributeValueOrNone "currentPosition" |> noneIfZero
          BeforePosition = dictionary |> tryGetAttributeValueOrNone "beforePosition" |> noneIfZero
          Score = dictionary |> tryGetAttributeValueOrNone "score" |> noneIfZero }

    type Extensions =
        [<Extension>]
        static member ToPoolGamblerScore(this: IDictionary<string, AttributeValue>) = toPoolGamblerScore this
