module FindPoolsUseCaseTests

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
let ``given an empty next token and an empty filter when FindActivePoolsLayoutsUseCase is executed then all of active PoolLayout are returned``
    ()
    =
    let poolLayoutRepositoryMock = Mock<IPoolRepository>()
    let poolLayoutId = Ulid.newUlid ()

    let asyncFindAndPaginateSignature =
        FuncAs.LinqExpression
            (fun (repository: IPoolRepository) ->
                repository.AsyncFindWithCursorPagination(
                    It.IsAny<PoolLayoutEntityPK>(),
                    It.IsAny<string option>(),
                    It.IsAny<string option>()
                ))

    poolLayoutRepositoryMock
        .Setup(asyncFindAndPaginateSignature)
        .Returns(
            Task.FromResult(
                { CursorPage.NextToken = None
                  Items =
                      [| { PoolEntity.PoolLayoutId = poolLayoutId |> Ulid.toString
                           Pk = ""
                           Sk = ""
                           PoolId = Ulid.newUlid () |> Ulid.toString
                           PoolName = "Copa América 2021 Pipel"
                           CurrentPosition = Nullable()
                           BeforePosition = Nullable()
                           Filter = "" } |] }
            )
            |> Async.AwaitTask
        )
    |> ignore

    let mapFunc =
        fun _ ->
            { Pool.PoolLayoutPK = { PoolLayoutPK.PoolLayoutId = poolLayoutId }
              PoolPK = { PoolPK.PoolId = Ulid.newUlid () }
              PoolName = NonEmptyString100.From "Copa América 2021 Pipel"
              CurrentPosition = None
              BeforePosition = None }

    let findPoolsUseCase =
        FindPoolsUseCase(poolLayoutRepositoryMock.Object, mapFunc) :> IFindPoolsUseCase

    let _ =
        findPoolsUseCase.AsyncExecute({ PoolLayoutPK.PoolLayoutId = poolLayoutId }, None, None)
        |> Async.RunSynchronously

    poolLayoutRepositoryMock.Verify(asyncFindAndPaginateSignature, Times.Once)
