module EmailTests

open System
open Xunit
open Felipearpa.Type

let emails: obj [] seq =
    seq { yield [| "email@email.email" |] }

let wrongValues: obj [] seq =
    seq {
        yield [| null |]
        yield [| "email" |]
        yield [| "email@email" |]
    }

[<Theory>]
[<MemberData(nameof emails)>]
let ``given a correct value for an email when a Email is created then a Email is returned`` (value: string) =
    let email = Email.newOf value
    Assert.True(true)
    Assert.Equal(value, email |> Email.value)

[<Theory>]
[<MemberData(nameof wrongValues)>]
let ``given a wrong value for an email when a Email is created then an exception is raised`` (value: string) =
    Assert.Throws<ArgumentException>(fun () -> Email.newOf value |> ignore)

[<Theory>]
[<MemberData(nameof emails)>]
let ``given a correct value for an email when a Email tries to create then an option with the Email is returned``
    (value: string)
    =
    let emailOpt = Email.tryOf value
    Assert.True(emailOpt |> Option.isSome)

    Assert.Equal(value, emailOpt.Value |> Email.value)

[<Theory>]
[<MemberData(nameof wrongValues)>]
let ``given a wrong value for an email when a Email tries to create then an option without value is returned``
    (value: string)
    =
    let emailOpt = Email.tryOf value
    Assert.True(emailOpt |> Option.isNone)
