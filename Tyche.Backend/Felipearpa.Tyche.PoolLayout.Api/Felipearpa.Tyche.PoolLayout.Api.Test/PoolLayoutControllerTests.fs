module PoolLayoutControllerTests

open System
open System.Net.Http
open System.Threading.Tasks
open Microsoft.AspNetCore.Mvc.Testing
open Microsoft.Extensions.DependencyInjection
open Moq
open Felipearpa.Core
open Felipearpa.Tyche.PoolLayout.Api
open Felipearpa.Tyche.PoolLayout.Domain.UseCases
open Felipearpa.Type
open Xunit
open Felipearpa.Tyche.PoolLayout.Domain

let createClientForFindActivePoolsLayoutsUseCase () =
    let findActivePoolsLayoutsUseCase =
        Mock<IFindActivePoolsLayoutsUseCase>()

    findActivePoolsLayoutsUseCase
        .Setup(fun x -> x.AsyncExecute(It.IsAny<string option>(), It.IsAny<string option>()))
        .Returns(
            Task.FromResult(
                { CursorPage.NextToken = None
                  Items =
                    [| { PoolLayout.PoolLayoutPK = { PoolLayoutPK.PoolLayoutId = Ulid.random () }
                         Name = NonEmptyString100.newOf "Copa América 2021"
                         OpeningStartDateTime = DateTime.Now
                         OpeningEndDateTime = DateTime.Now } |] }
            )
            |> Async.AwaitTask
        )
    |> ignore

    let application =
        (new WebApplicationFactory<Program>())
            .WithWebHostBuilder(fun builder ->
                builder.ConfigureServices (fun services ->
                    services.AddTransient<IFindActivePoolsLayoutsUseCase> (fun provider ->
                        findActivePoolsLayoutsUseCase.Object)
                    |> ignore)
                |> ignore)

    application.CreateClient()

[<Fact>]
let ``given a valid skip and take when a request to query the list of active PoolLayout is executed then a list of active PoolLayout is returned``
    ()
    =
    let client =
        createClientForFindActivePoolsLayoutsUseCase ()

    let request =
        new HttpRequestMessage(HttpMethod("GET"), "poolLayout/findActivePoolsLayouts?skip=0&take=1")

    let response =
        client.SendAsync(request)
        |> Async.AwaitTask
        |> Async.RunSynchronously

    response.EnsureSuccessStatusCode() |> ignore
