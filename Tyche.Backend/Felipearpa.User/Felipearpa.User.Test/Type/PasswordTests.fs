module PasswordTests

open System
open Felipearpa.User.Type
open Xunit

let validPasswords: obj[] seq =
    seq {
        yield [| "Password1#" |]
        yield [| "$1LongPassword1$" |]
    }

let invalidPasswords: obj[] seq =
    seq {
        yield [| null |]
        yield [| "" |]
        yield [| "Pass1#" |]
        yield [| "$1LongPassword1$&" |]
        yield [| "password" |]
        yield [| "Password" |]
        yield [| "PASSWORD" |]
    }

[<Theory>]
[<MemberData(nameof validPasswords)>]
let ``given a valid password when a Password instance is created then the instance is returned``
    (validPassword: string)
    =
    let createdPassword =
        validPassword |> Password.newOf

    Assert.Equal(validPassword, createdPassword |> Password.value)

[<Theory>]
[<MemberData(nameof invalidPasswords)>]
let ``given an invalid password when a Password instance is created then an exception is raised``
    (invalidPassword: string)
    =
    Assert.Throws<ArgumentException>(fun () -> Password.newOf invalidPassword |> ignore)

[<Theory>]
[<MemberData(nameof validPasswords)>]
let ``given a valid password when a Password instance is tried to create then an option with the instance is returned``
    (validPassword: string)
    =
    let maybePassword =
        validPassword |> Password.tryOf

    Assert.True(maybePassword |> Option.isSome)
    Assert.Equal(validPassword, maybePassword.Value |> Password.value)

[<Theory>]
[<MemberData(nameof invalidPasswords)>]
let ``given an invalid password when a Password tries to create then an option without value is returned``
    (invalidPassword: string)
    =
    let maybePassword =
        Password.tryOf invalidPassword

    Assert.True(maybePassword |> Option.isNone)
