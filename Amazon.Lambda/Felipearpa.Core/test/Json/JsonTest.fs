module JsonTest

open FsUnitTyped
open Xunit
open Felipearpa.Core
open Felipearpa.Core.Json

type Data = { a: string }

[<Fact>]
let ``given an object when it's serialized and after deserialized then the object is equal to deserialized object`` () =
    let serializer = JsonSerializer() :> ISerializer

    let obj = {| Code = "c1"; Name = "Code c1" |}

    let serializedObj = serializer.Serialize(obj)

    let deserializedObj = serializer.Deserialize(serializedObj)

    obj |> shouldEqual deserializedObj

[<Fact>]
let ``given a json string that not represent the object to deserialize when is deserialized then null is returned`` () =
    let serializer = JsonSerializer() :> ISerializer
    let json = "{ \"b\": \"value\"}"
    (fun () -> serializer.Deserialize<Data>(json) |> ignore) |> shouldFail
