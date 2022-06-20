namespace Pipel.Data.DynamoDb

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Pipel.Core

type IKeySerializer =

    abstract Serialize: IDictionary<string, AttributeValue> -> string

    abstract Deserialize: string -> IDictionary<string, AttributeValue>

type KeySerializer(serializer: ISerializer) =

    interface IKeySerializer with

        member this.Serialize dictionary =
            serializer.Serialize(
                {| Pk = dictionary["pk"].S
                   Sk = dictionary["sk"].S |}
            )

        member this.Deserialize value =
            let dictionary =
                serializer.Deserialize<IDictionary<string, obj>>(value)

            dict [ "pk", AttributeValue(dictionary[ "pk" ].ToString())
                   "sk", AttributeValue(dictionary[ "sk" ].ToString()) ]
