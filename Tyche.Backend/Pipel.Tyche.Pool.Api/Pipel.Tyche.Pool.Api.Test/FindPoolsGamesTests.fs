module FindPoolsGamesTests

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

let createClientForFindPoolsGamesUseCase () =
    let findPoolsGamesUseCaseMock =
        Mock<IFindPoolsGamesUseCase>()

    findPoolsGamesUseCaseMock
        .Setup(fun x -> x.AsyncExecute(It.IsAny<PoolPK>(), It.IsAny<string option>(), It.IsAny<string option>()))
        .Returns(
            Task.FromResult(
                { CursorPage.NextToken = None
                  Items =
                    [| { PoolGame.PoolGamePK =
                           { PoolGamePK.PoolPK = { PoolPK.PoolId = Ulid.newUlid () }
                             GamePK = { GamePK.GameId = Ulid.newUlid () } }
                         HomeTeamPK = { TeamPK.TeamId = Ulid.newUlid () }
                         HomeTeamName = NonEmptyString100.From "Colombia"
                         HomeTeamScore = PositiveInt.TryFrom 1
                         HomeTeamBet = PositiveInt.TryFrom 1
                         AwayTeamPK = { TeamPK.TeamId = Ulid.newUlid () }
                         AwayTeamName = NonEmptyString100.From "Brasil"
                         AwayTeamScore = PositiveInt.TryFrom 1
                         AwayTeamBet = PositiveInt.TryFrom 1
                         BetScore = PositiveInt.TryFrom 1
                         MatchDateTime = DateTime.now () } |] }
            )
            |> Async.AwaitTask
        )
    |> ignore

    let application =
        (new WebApplicationFactory<Program>())
            .WithWebHostBuilder(fun builder ->
                builder.ConfigureServices (fun services ->
                    services.AddTransient<IFindPoolsGamesUseCase>(fun provider -> findPoolsGamesUseCaseMock.Object)
                    |> ignore)
                |> ignore)

    application.CreateClient()

[<Fact>]
let ``given a valid request when a request to query the list of PoolGame is executed then a list of PoolGame is returned``
    ()
    =
    let client =
        createClientForFindPoolsGamesUseCase ()

    let request =
        new HttpRequestMessage(HttpMethod("GET"), "pool/findPoolsGames?poolId=01FQWJ2KC6HPFPB0CWBM624SHA")

    let response =
        client.SendAsync(request)
        |> Async.AwaitTask
        |> Async.RunSynchronously

    response.EnsureSuccessStatusCode() |> ignore
