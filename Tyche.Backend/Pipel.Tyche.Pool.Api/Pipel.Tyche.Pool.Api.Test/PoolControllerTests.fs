module PoolControllerTests

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

let createClientForFindActivePoolsLayoutsUseCase () =
    let findPoolsUseCase = Mock<IFindPoolsUseCase>()

    findPoolsUseCase
        .Setup(fun x -> x.AsyncExecute(It.IsAny<PoolLayoutPK>(), It.IsAny<string option>(), It.IsAny<string option>()))
        .Returns(
            Task.FromResult(
                { CursorPage.NextToken = None
                  Items =
                      [| { Pool.PoolLayoutPK = { PoolLayoutPK.PoolLayoutId = Ulid.newUlid () }
                           PoolPK = { PoolPK.PoolId = Ulid.newUlid () }
                           PoolName = NonEmptyString100.From "Copa Ameérica 2022"
                           CurrentPosition = None
                           BeforePosition = None } |] }
            )
            |> Async.AwaitTask
        )
    |> ignore

    let application =
        (new WebApplicationFactory<Program>())
            .WithWebHostBuilder(fun builder ->
                builder.ConfigureServices
                    (fun services ->
                        services.AddTransient<IFindPoolsUseCase>(fun provider -> findPoolsUseCase.Object)
                        |> ignore)
                |> ignore)

    application.CreateClient()

[<Fact>]
let ``given a valid request when a request to query the list of Pool is executed then a list of Pool is returned`` () =
    let client =
        createClientForFindActivePoolsLayoutsUseCase ()

    let request =
        new HttpRequestMessage(HttpMethod("GET"), "pool/findPools?poolLayoutId=01FQWJ2KC6HPFPB0CWBM624SHA")

    let response =
        client.SendAsync(request)
        |> Async.AwaitTask
        |> Async.RunSynchronously

    response.EnsureSuccessStatusCode() |> ignore
