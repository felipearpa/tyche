module UsernameTests

open System
open Felipearpa.User.Type
open Xunit

let validUsernames: obj[] seq =
    seq {
        yield [| "username" |]
        yield [| "username123" |]
        yield [| "123username" |]
        yield [| "123username123" |]
        yield [| "username___" |]
        yield [| "___username" |]
        yield [| "___username___" |]
        yield [| "username---" |]
        yield [| "---username" |]
        yield [| "---username---" |]
        yield [| "123" |]
        yield [| "1234567890123456" |]
    }

let invalidUsernames: obj[] seq =
    seq {
        yield [| null |]
        yield [| "" |]
        yield [| "12" |]
        yield [| "12345678901234567" |]
        yield [| "username." |]
        yield [| "username@" |]
        yield [| "username$" |]
        yield [| "Username$" |]
        yield [| "user name" |]
    }

[<Theory>]
[<MemberData(nameof validUsernames)>]
let ``given a valid username when an Username instance is created then the instance is returned``
    (validUsername: string)
    =
    let createdUsername =
        validUsername |> Username.newOf

    Assert.Equal(validUsername, createdUsername |> Username.value)

[<Theory>]
[<MemberData(nameof invalidUsernames)>]
let ``given an invalid username when an Username instance is created then an exception is raised``
    (invalidUsername: string)
    =
    Assert.Throws<ArgumentException>(fun () -> Username.newOf invalidUsername |> ignore)

[<Theory>]
[<MemberData(nameof validUsernames)>]
let ``given a valid username when an UserName instance is tried to create then an option with the instance is returned``
    (validUsername: string)
    =
    let maybeUsername =
        validUsername |> Username.tryOf

    Assert.True(maybeUsername |> Option.isSome)
    Assert.Equal(validUsername, maybeUsername.Value |> Username.value)

[<Theory>]
[<MemberData(nameof invalidUsernames)>]
let ``given an invalid username when an Username tries to create then an option without value is returned``
    (invalidUsername: string)
    =
    let maybeUsername =
        Username.tryOf invalidUsername

    Assert.True(maybeUsername |> Option.isNone)
