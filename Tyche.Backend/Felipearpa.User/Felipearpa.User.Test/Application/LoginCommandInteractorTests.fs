module LoginCommandHandlerTests

open System.Threading.Tasks
open Felipearpa.Crypto
open Felipearpa.Type
open Felipearpa.User.Application.Login
open Felipearpa.User.Domain
open Felipearpa.User.Type
open Microsoft.FSharp.Control
open Moq
open Xunit

let fakeUserRepository () =
    let userRepository = Mock<IUserRepository>()

    userRepository
        .Setup(fun repository -> repository.LoginAsync(It.IsAny<Username>(), It.IsAny<string>()))
        .Returns(
            Task.FromResult(
                { User.UserId = Ulid.random ()
                  Username = "felipearpa" |> Username.newOf
                  Hash = "hash" }
                |> Ok
            )
            |> Async.AwaitTask
        )
    |> ignore

    userRepository

let fakeHasher () =
    let hasher = Mock<IHasher>()

    hasher.Setup(fun hasher -> hasher.Hash(It.IsAny<string>())).Returns("hash")
    |> ignore

    hasher

[<Fact>]
let ``given an username and password when a command to login the user is performed then an user is logged in`` () =
    let userRepository = fakeUserRepository ()
    let hasher = fakeHasher ()

    let loginCommandHandler = LoginCommandHandler(userRepository.Object)

    let _ =
        loginCommandHandler.ExecuteAsync(
            { LoginCommand.Username = "username" |> Username.newOf
              Password = "#1Password1#" |> Password.newOf }
        )
        |> Async.RunSynchronously

    userRepository.Verify(fun userRepository -> userRepository.LoginAsync(It.IsAny<Username>(), It.IsAny<string>()))
