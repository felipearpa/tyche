module KeySerializerTests

open System.Collections.Generic
open Amazon.DynamoDBv2.Model
open Moq
open Felipearpa.Core
open Felipearpa.Data.DynamoDb
open Xunit

[<Fact>]
let ``given data to serialize when it's serialized then a string that represents data is returned`` () =
    let data =
        dict [ "pk", AttributeValue("pk")
               "sk", AttributeValue("sk") ]

    let serializerMock = Mock<ISerializer>()

    let serializeSignature =
        FuncAs.LinqExpression(fun (x: ISerializer) -> x.Serialize(It.IsAny()))

    serializerMock
        .Setup(serializeSignature)
        .Returns("serialized string")
    |> ignore

    let keySerializer =
        DynamoDbKeySerializer(serializerMock.Object) :> IKeySerializer

    let _ = keySerializer.Serialize data

    serializerMock.Verify(serializeSignature)

[<Fact>]
let ``given data to deserialize when it's deserialized then a dictionary that represents data is returned`` () =
    let data = "string to deserialize"
    let serializerMock = Mock<ISerializer>()

    let deserializeSignature =
        FuncAs.LinqExpression(fun (x: ISerializer) -> x.Deserialize<IDictionary<string, obj>>(It.IsAny()))

    serializerMock
        .Setup(deserializeSignature)
        .Returns(
            dict [ "pk", "pk" :> obj
                   "sk", "sk" ]
        )
    |> ignore

    let keySerializer =
        DynamoDbKeySerializer(serializerMock.Object) :> IKeySerializer

    let _ = keySerializer.Deserialize data

    serializerMock.Verify(deserializeSignature)
