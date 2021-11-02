module PositiveIntTests

open System
open Pipel.Type
open Xunit

let wrongValues: obj [] seq = seq { yield [| -1 |] }

[<Fact>]
let ``given a correct value for a PositiveInt when a PositiveInt is created then a PositiveInt is returned`` () =
    let value = 1
    let _ = PositiveInt.From value
    Assert.True(true)

[<Theory>]
[<MemberData(nameof (wrongValues))>]
let ``given a wrong value for a PositiveInt when a PositiveInt is created then an exception is raised`` (value: int32) =
    Assert.Throws<ArgumentException>(fun () -> PositiveInt.From value |> ignore)

[<Fact>]
let ``given a correct value for a PositiveInt when a PositiveInt tries to create then an option with the value is returned``
    ()
    =
    let value = 1
    let positiveInt32Opt = PositiveInt.TryFrom value
    Assert.True(positiveInt32Opt.IsSome)
    Assert.Equal(value, positiveInt32Opt.Value |> PositiveInt.value)

[<Theory>]
[<MemberData(nameof wrongValues)>]
let ``given a wrong value for a PositiveInt when a PositiveInt tries to create then an option without value is returned``
    (value: int32)
    =
    let positiveInt32Opt = PositiveInt.TryFrom value
    Assert.True(positiveInt32Opt.IsNone)

[<Fact>]
let ``given two correct values for PositiveInt when a numeric operation is applied with then an correct value is returned``
    ()
    =

    let a = 10
    let b = 5
    let aPositiveInt = PositiveInt.From a
    let bPositiveInt = PositiveInt.From b

    Assert.Equal(a + b, (aPositiveInt + bPositiveInt) |> PositiveInt.value)
    Assert.Equal(a - b, (aPositiveInt - bPositiveInt) |> PositiveInt.value)
    Assert.Equal(a * b, (aPositiveInt * bPositiveInt) |> PositiveInt.value)
    Assert.Equal(a / b, (aPositiveInt / bPositiveInt) |> PositiveInt.value)
