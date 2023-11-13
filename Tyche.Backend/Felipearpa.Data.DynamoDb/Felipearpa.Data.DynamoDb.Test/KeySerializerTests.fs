module KeySerializerTests

open System.Linq.Expressions
open System
open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Moq
open Felipearpa.Core
open Felipearpa.Data.DynamoDb
open Xunit

let buildSerializer (signature: Expression<Func<ISerializer, string>>) =
    let serializer = Mock<ISerializer>()

    serializer.Setup(signature).Returns("serialized string") |> ignore

    serializer

let buildDeserializer (signature: Expression<Func<ISerializer, IDictionary<string, obj>>>) =
    let deserializer = Mock<ISerializer>()

    deserializer.Setup(signature).Returns(dict [ "pk", "pk" :> obj; "sk", "sk" ])
    |> ignore

    deserializer

[<Fact>]
let ``given data to serialize when it's serialized then a string that represents data is returned`` () =
    let data = dict [ "pk", AttributeValue("pk"); "sk", AttributeValue("sk") ]

    let serializeSignature =
        FuncAs.LinqExpression(fun (x: ISerializer) -> x.Serialize(It.IsAny()))

    let serializer = buildSerializer serializeSignature

    let keySerializer = DynamoDbKeySerializer(serializer.Object) :> IKeySerializer

    let _ = keySerializer.Serialize data

    serializer.Verify(serializeSignature)

[<Fact>]
let ``given data to deserialize when it's deserialized then a dictionary that represents data is returned`` () =
    let data = "string to deserialize"

    let deserializeSignature =
        FuncAs.LinqExpression(fun (x: ISerializer) -> x.Deserialize<IDictionary<string, obj>>(It.IsAny()))

    let deserializer = buildDeserializer deserializeSignature

    let keySerializer = DynamoDbKeySerializer(deserializer.Object) :> IKeySerializer

    let _ = keySerializer.Deserialize data

    deserializer.Verify(deserializeSignature)
