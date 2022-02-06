module UlidTests

open System
open Xunit
open Pipel.Type

[<Fact>]
let ``given a System.Ulid when an Ulid is created from it then an Ulid is returned`` () =
    let value = Ulid.NewUlid()
    let uuid = Ulid.From value
    Assert.Equal(value, uuid |> Ulid.value)

[<Fact>]
let ``given a valid string when an Ulid is created from it then an Ulid is returned`` () =
    let value = Ulid.NewUlid().ToString()
    let ulid = Ulid.From value
    Assert.Equal(value, ulid |> Ulid.toString)

[<Fact>]
let ``given a invalid string when an Ulid is created from it then an exception is raised`` () =
    let value = ""
    Assert.Throws<ArgumentException>(fun () -> Ulid.From value |> ignore)

[<Fact>]
let ``given a valid string when an Ulid tries to create from it then an Ulid option with value is returned`` () =
    let value = Ulid.NewUlid().ToString()
    let ulidOpt = Ulid.TryFrom value
    Assert.True(ulidOpt.IsSome)
    Assert.Equal(value, ulidOpt.Value |> Ulid.toString)

[<Fact>]
let ``given a invalid string when an Ulid tries to create from it then an Ulid option without value is returned`` () =
    let value = ""
    let ulidOpt = Ulid.TryFrom value
    Assert.True(ulidOpt.IsNone)
