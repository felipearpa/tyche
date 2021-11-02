module DateTimeTests

open System
open Xunit
open Pipel.Type

[<Fact>]
let ``given a System.DateTime when a DateTime is created then a DateTime is returned`` () =
    let value = System.DateTime.Now
    let dateTime = DateTime.From value
    Assert.True(true)
    Assert.Equal(value, dateTime |> DateTime.value)

[<Fact>]
let ``given two DateTime when they are compared then the correct value is returned`` () =
    let a = DateTime.now ()
    let b = a
    let c = a |> DateTime.addDays 1.0

    (a = b) |> Assert.True
    (a.Equals b) |> Assert.True
    (a >= b) |> Assert.True
    (a <= b) |> Assert.True
    (a <> b) |> Assert.False
    (a > b) |> Assert.False
    (a < b) |> Assert.False

    (a = c) |> Assert.False
    (a.Equals c) |> Assert.False
    (a >= c) |> Assert.False
    (a <= c) |> Assert.True
    (a <> c) |> Assert.True
    (a > c) |> Assert.False
    (a < c) |> Assert.True
