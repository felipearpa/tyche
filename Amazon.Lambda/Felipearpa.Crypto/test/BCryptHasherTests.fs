module BcryptHasherTests

open Felipearpa.Crypto
open Xunit

[<Fact>]
let ``given a string when is hashed by using BCryptHascher then a new string is returned`` () =
    let sourceString = "password"
    let hasher: IHasher = BCryptHasher()
    let newString = sourceString |> hasher.Hash
    Assert.NotNull newString
    Assert.NotEmpty newString
    Assert.NotEqual<string>(newString, sourceString)
