namespace Felipearpa.Data.DynamoDb

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Felipearpa.Core

type IKeySerializer =

    abstract Serialize: IDictionary<string, AttributeValue> -> string

    abstract Deserialize: string -> IDictionary<string, AttributeValue>

type DynamoDbKeySerializer(serializer: ISerializer) =

    interface IKeySerializer with

        member this.Serialize dictionary =
            serializer.Serialize(
                {| Pk = dictionary[Key.pk].S
                   Sk = dictionary[Key.sk].S |}
            )

        member this.Deserialize value =
            let dictionary = serializer.Deserialize<IDictionary<string, obj>>(value)

            dict
                [ Key.pk, AttributeValue(dictionary[Key.pk].ToString())
                  Key.sk, AttributeValue(dictionary[Key.sk].ToString()) ]
