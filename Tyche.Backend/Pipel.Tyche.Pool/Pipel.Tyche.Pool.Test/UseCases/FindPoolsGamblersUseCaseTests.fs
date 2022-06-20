module FindPoolsGamblersUseCaseTests

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
let ``given an empty next token and an empty filter when FindActivePoolsGamblersUseCase is executed then all of PoolGambler are returned``
    ()
    =
    let poolGamblerRepositoryMock =
        Mock<IPoolGamblerRepository>()

    let poolId = Ulid.newUlid ()
    let gamblerId = Ulid.newUlid ()

    let asyncFindAndPaginateSignature =
        FuncAs.LinqExpression (fun (repository: IPoolGamblerRepository) ->
            repository.AsyncFindWithCursorPagination(It.IsAny(), It.IsAny(), It.IsAny()))

    poolGamblerRepositoryMock
        .Setup(asyncFindAndPaginateSignature)
        .Returns(
            Task.FromResult(
                { CursorPage.NextToken = None
                  Items =
                    [| { PoolGamblerEntity.Pk = ""
                         Sk = ""
                         PoolId = poolId |> Ulid.toString
                         GamblerId = gamblerId |> Ulid.toString
                         GamblerEmail = "email@email.com"
                         Score = Nullable(10)
                         CurrentPosition = Nullable(1)
                         BeforePosition = Nullable(2)
                         Filter = "" } |] }
            )
            |> Async.AwaitTask
        )
    |> ignore

    let mapFunc =
        fun _ ->
            { PoolGamblerPK =
                { PoolPK = { PoolPK.PoolId = poolId }
                  GamblerPK = { GamblerPK.GamblerId = gamblerId } }
              GamblerEmail = Email.From "email@email.com"
              Score = PositiveInt.TryFrom 10
              CurrentPosition = PositiveInt.TryFrom 1
              BeforePosition = PositiveInt.TryFrom 2 }

    let findPoolsGamblersUseCase =
        FindPoolsGamblersUseCase(poolGamblerRepositoryMock.Object, mapFunc) :> IFindPoolsGamblersUseCase

    let _ =
        findPoolsGamblersUseCase.AsyncExecute({ PoolPK.PoolId = poolId }, None, None)
        |> Async.RunSynchronously

    poolGamblerRepositoryMock.Verify(asyncFindAndPaginateSignature, Times.Once)
