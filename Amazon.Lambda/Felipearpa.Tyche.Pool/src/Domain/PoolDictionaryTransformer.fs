namespace Felipearpa.Tyche.Pool.Domain

open System.Collections.Generic
open System.Runtime.CompilerServices
open Amazon.DynamoDBv2.Model
open Felipearpa.Type

module PoolDictionaryTransformer =
    let toPool (dictionary: IDictionary<string, AttributeValue>) =
        { Pool.PoolId = dictionary["poolId"].S |> Ulid.newOf
          PoolName = dictionary["poolName"].S |> NonEmptyString100.newOf
          PoolLayoutId = dictionary["poolLayoutId"].S |> Ulid.newOf }

    type Extensions =
        [<Extension>]
        static member ToPool(this: IDictionary<string, AttributeValue>) = toPool this
