module UlidTests

open System
open Xunit
open Felipearpa.Type

[<Fact>]
let ``given a valid string when an Ulid is created from it then an Ulid is returned`` () =
    let value = Ulid.NewUlid().ToString()
    let ulid = Ulid.newOf value
    Assert.Equal(value, ulid.ToString())

[<Fact>]
let ``given a invalid string when an Ulid is created from it then an exception is raised`` () =
    let value = ""
    Assert.Throws<ArgumentException>(fun () -> Ulid.newOf value |> ignore)

[<Fact>]
let ``given a valid string when an Ulid tries to create from it then an Ulid option with value is returned`` () =
    let value = Ulid.NewUlid().ToString()
    let ulidOpt = Ulid.tryOf value
    Assert.True(ulidOpt |> Option.isSome)
    Assert.Equal(value, ulidOpt.Value |> Ulid.value)

[<Fact>]
let ``given a invalid string when an Ulid tries to create from it then an Ulid option without value is returned`` () =
    let value = ""
    let ulidOpt = Ulid.tryOf value
    Assert.True(ulidOpt |> Option.isNone)
