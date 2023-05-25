module JsonTest

open Xunit
open Felipearpa.Core
open Felipearpa.Core.Json

[<Fact>]
let ``given an object when it's serialized and after deserialized then the object is equal to deserialized object`` () =
    let jsonSerializer = JsonSerializer() :> ISerializer

    let obj =
        {| Code = "c1"
           Name = "Code c1" |}

    let serializedObj = jsonSerializer.Serialize(obj)

    let deserializedObj =
        jsonSerializer.Deserialize(serializedObj)

    Assert.Equal(obj, deserializedObj)
