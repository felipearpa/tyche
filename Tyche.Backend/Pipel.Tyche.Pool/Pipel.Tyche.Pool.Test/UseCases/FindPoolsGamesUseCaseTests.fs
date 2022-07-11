module FindPoolsGamesUseCaseTests

open System
open System.Threading.Tasks
open Moq
open Pipel.Core
open Pipel.Tyche.Pool.Data
open Pipel.Tyche.Pool.Domain
open Pipel.Tyche.Pool.Domain.UseCases
open Pipel.Type
open Xunit

[<Fact>]
let ``given an empty next token and an empty filter when FindPoolsGamesUseCase is executed then all of PoolsGames are returned``
    ()
    =
    let poolGameRepositoryMock =
        Mock<IPoolGameRepository>()

    let poolId = Ulid.newUlid ()
    let gameId = Ulid.newUlid ()

    let asyncFindAndPaginateSignature =
        FuncAs.LinqExpression (fun (repository: IPoolGameRepository) ->
            repository.AsyncFind(It.IsAny(), It.IsAny(), It.IsAny()))

    poolGameRepositoryMock
        .Setup(asyncFindAndPaginateSignature)
        .Returns(
            Task.FromResult(
                { CursorPage.NextToken = None
                  Items =
                    [| { PoolGameEntity.Pk = ""
                         Sk = ""
                         PoolId = poolId |> Ulid.toString
                         GameId = gameId |> Ulid.toString
                         HomeTeamId = Ulid.newUlid () |> Ulid.toString
                         HomeTeamName = "Colombia"
                         HomeTeamScore = Nullable(1)
                         HomeTeamBet = Nullable(2)
                         AwayTeamId = Ulid.newUlid () |> Ulid.toString
                         AwayTeamName = "Brasil"
                         AwayTeamScore = Nullable(3)
                         AwayTeamBet = Nullable(2)
                         BetScore = Nullable(10)
                         MatchDateTime = DateTime.Now
                         Filter = "" } |] }
            )
            |> Async.AwaitTask
        )
    |> ignore

    let mapFunc =
        fun _ ->
            { PoolGamePK =
                { PoolPK = { PoolPK.PoolId = poolId }
                  GamePK = { GamePK.GameId = gameId } }
              HomeTeamPK = { TeamPK.TeamId = Ulid.newUlid () }
              HomeTeamName = NonEmptyString100.From "Colombia"
              HomeTeamScore = PositiveInt.TryFrom 1
              HomeTeamBet = PositiveInt.TryFrom 2
              AwayTeamPK = { TeamPK.TeamId = Ulid.newUlid () }
              AwayTeamName = NonEmptyString100.From "Brasil"
              AwayTeamScore = PositiveInt.TryFrom 3
              AwayTeamBet = PositiveInt.TryFrom 2
              BetScore = PositiveInt.TryFrom 10
              MatchDateTime = DateTime.now () }

    let findPoolsGamesUseCase =
        FindPoolsGamesUseCase(poolGameRepositoryMock.Object, mapFunc) :> IFindPoolsGamesUseCase

    let _ =
        findPoolsGamesUseCase.AsyncExecute({ PoolPK.PoolId = poolId }, None, None)
        |> Async.RunSynchronously

    poolGameRepositoryMock.Verify(asyncFindAndPaginateSignature, Times.Once)
