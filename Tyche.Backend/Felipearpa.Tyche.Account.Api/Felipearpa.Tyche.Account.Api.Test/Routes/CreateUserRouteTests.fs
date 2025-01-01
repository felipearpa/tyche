module CreateUserRouteTests

open System.Net.Http
open System.Net.Http.Json
open System.Threading.Tasks
open Felipearpa.Crypto
open Felipearpa.Type
open Felipearpa.User.Api
open Felipearpa.User.Api.Request.UserCreation
open Felipearpa.User.Application.UserCreation
open Felipearpa.User.Domain
open Felipearpa.User.Type
open Microsoft.AspNetCore.Mvc.Testing
open Microsoft.Extensions.DependencyInjection
open Moq
open Xunit

let fakeUserRepository () =
    let userRepository = Mock<IUserRepository>()

    userRepository
        .Setup(fun repository -> repository.CreateAsync(It.IsAny<User>()))
        .Returns(
            Task.FromResult(
                { User.UserId = Ulid.random ()
                  Email = "username" |> Username.newOf
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

let createWebClient () =
    let fakeUserRepository = fakeUserRepository ()
    let fakeHasher = fakeHasher ()

    let application =
        (new WebApplicationFactory<Program>())
            .WithWebHostBuilder(fun builder ->
                builder.ConfigureServices(fun services ->
                    services.AddTransient<CreateUserCommandHandler>(fun provider ->
                        CreateUserCommandHandler(fakeUserRepository.Object, fakeHasher.Object))
                    |> ignore)
                |> ignore)

    application.CreateClient()

[<Fact>]
let ``given a valid request to create an user when the request is performed then a success response is gotten`` () =
    let client = createWebClient ()

    let request = new HttpRequestMessage(HttpMethod("POST"), "user")

    request.Content <-
        JsonContent.Create(
            { CreateUserRequest.Email = "felipearpa"
              Password = "#1Password1#" }
        )

    let response =
        client.SendAsync(request) |> Async.AwaitTask |> Async.RunSynchronously

    response.EnsureSuccessStatusCode() |> ignore
