module FindPoolsGamblersTests

open System.Net.Http
open System.Threading.Tasks
open Microsoft.AspNetCore.Mvc.Testing
open Microsoft.Extensions.DependencyInjection
open Moq
open Pipel.Core
open Pipel.Tyche.Pool.Api
open Pipel.Tyche.Pool.Domain.UseCases
open Pipel.Type
open Xunit
open Pipel.Tyche.Pool.Domain

let createClientForFindPoolsGamblersUseCase () =
    let findPoolsGamblersUseCaseMock =
        Mock<IFindPoolsGamblersUseCase>()

    findPoolsGamblersUseCaseMock
        .Setup(fun x -> x.AsyncExecute(It.IsAny<PoolPK>(), It.IsAny<string option>(), It.IsAny<string option>()))
        .Returns(
            Task.FromResult(
                { CursorPage.NextToken = None
                  Items =
                    [| { PoolGambler.PoolGamblerPK =
                           { PoolGamblerPK.PoolPK = { PoolPK.PoolId = Ulid.newUlid () }
                             GamblerPK = { GamblerPK.GamblerId = Ulid.newUlid () } }
                         GamblerEmail = Email.From "email@email.com"
                         Score = PositiveInt.TryFrom 10
                         CurrentPosition = PositiveInt.TryFrom 1
                         BeforePosition = PositiveInt.TryFrom 2 } |] }
            )
            |> Async.AwaitTask
        )
    |> ignore

    let application =
        (new WebApplicationFactory<Program>())
            .WithWebHostBuilder(fun builder ->
                builder.ConfigureServices (fun services ->
                    services.AddTransient<IFindPoolsGamblersUseCase>(fun provider -> findPoolsGamblersUseCaseMock.Object)
                    |> ignore)
                |> ignore)

    application.CreateClient()

[<Fact>]
let ``given a valid request when a request to query the list of PoolGambler is executed then a list of PoolGambler is returned`` () =
    let client =
        createClientForFindPoolsGamblersUseCase ()

    let request =
        new HttpRequestMessage(HttpMethod("GET"), "pool/findPoolsGamblers?poolId=01FQWJ2KC6HPFPB0CWBM624SHA")

    let response =
        client.SendAsync(request)
        |> Async.AwaitTask
        |> Async.RunSynchronously

    response.EnsureSuccessStatusCode() |> ignore
