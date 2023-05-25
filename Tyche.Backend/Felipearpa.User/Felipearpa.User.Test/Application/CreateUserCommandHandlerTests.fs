module CreateUserCommandHandlerTests

open System.Threading.Tasks
open Felipearpa.Crypto
open Felipearpa.Type
open Felipearpa.User.Application.UserCreation
open Felipearpa.User.Domain
open Felipearpa.User.Type
open Microsoft.FSharp.Control
open Moq
open Xunit

let fakeUserRepository () =
    let userRepository = Mock<IUserRepository>()

    userRepository
        .Setup(fun repository -> repository.CreateAsync(It.IsAny<User>()))
        .Returns(
            Task.FromResult(
                { User.UserId = Ulid.random ()
                  Username = "username" |> Username.newOf
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
let ``given an username and password when a command to create an user is performed then an user is created`` () =
    let userRepositoryFake = fakeUserRepository ()
    let hasherFake = fakeHasher ()

    let createUserCommandHandler =
        CreateUserCommandHandler(userRepositoryFake.Object, hasherFake.Object)

    let _ =
        createUserCommandHandler.ExecuteAsync(
            { CreateUserCommand.Username = "username" |> Username.newOf
              Password = "#1Password1#" |> Password.newOf }
        )
        |> Async.RunSynchronously

    userRepositoryFake.Verify(fun userRepository -> userRepository.CreateAsync(It.IsAny<User>()))
