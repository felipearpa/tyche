module UuidTests

open System
open Xunit
open Felipearpa.Type

[<Fact>]
let ``given a Guid when an Uuid is created from it then an Uuid is returned`` () =
    let value = Guid.NewGuid().ToString()
    let uuid = Uuid.newOf value
    Assert.Equal(value, uuid |> Uuid.value)

[<Fact>]
let ``given a invalid string when an Uuid is created from it then an exception is raised`` () =
    let value = ""
    Assert.Throws<ArgumentException>(fun () -> Uuid.newOf value |> ignore)

[<Fact>]
let ``given a valid string when an Uuid tries to create from it then an Uuid option with value is returned`` () =
    let value = Guid.NewGuid().ToString()
    let uuidOpt = Uuid.tryOf value
    Assert.True(uuidOpt |> Option.isSome)
    Assert.Equal(value, uuidOpt.Value |> Uuid.toString)

[<Fact>]
let ``given a invalid string when an Uuid tries to create from it then an Uuid option without value is returned`` () =
    let value = ""
    let uuidOpt = Uuid.tryOf value
    Assert.True(uuidOpt |> Option.isNone)
