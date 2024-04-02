namespace Felipearpa.Tyche.Pool.Domain

open System
open System.Collections.Generic
open System.Runtime.CompilerServices
open Amazon.DynamoDBv2.Model
open Felipearpa.Type

module PoolGamblerScoreDictionaryTransformer =
    let toPoolGamblerScore (dictionary: IDictionary<string, AttributeValue>) =
        { PoolGamblerScore.PoolId = dictionary["poolId"].S |> Ulid.newOf
          GamblerId = dictionary["gamblerId"].S |> Ulid.newOf
          PoolName = dictionary["poolName"].S |> NonEmptyString100.newOf
          GamblerUsername = dictionary["gamblerUsername"].S |> NonEmptyString100.newOf
          CurrentPosition = dictionary["currentPosition"].N |> Option.ofObj |> Option.map Int32.Parse
          BeforePosition = dictionary["beforePosition"].N |> Option.ofObj |> Option.map Int32.Parse
          Score = dictionary["score"].N |> Option.ofObj |> Option.map Int32.Parse }

    type Extensions =
        [<Extension>]
        static member ToPoolGamblerScore(this: IDictionary<string, AttributeValue>) =
            toPoolGamblerScore this
