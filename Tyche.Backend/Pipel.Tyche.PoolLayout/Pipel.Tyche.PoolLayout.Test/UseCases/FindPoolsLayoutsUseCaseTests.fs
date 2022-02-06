module FindPoolsLayoutsUseCaseTests

open System
open System.Threading.Tasks
open Moq
open Pipel.Core
open Pipel.Data.DynamoDb
open Pipel.Tyche.PoolLayout.Data
open Pipel.Tyche.PoolLayout.Domain
open Pipel.Tyche.PoolLayout.Domain.UseCases
open Pipel.Type
open Xunit

[<Fact>]
let ``given an empty next token and an empty filter when FindActivePoolsLayoutsUseCase is executed then all of active PoolLayout are returned``
    ()
    =
    let poolLayoutRepositoryMock = Mock<IPoolLayoutRepository>()

    let asyncFindAndPaginateSignature =
        FuncAs.LinqExpression
            (fun (repository: IPoolLayoutRepository) ->
                repository.AsyncFindWithCursorPagination(
                    It.IsAny<string option>(),
                    It.IsAny<string option>(),
                    It.IsAny<DbFilter option>()
                ))

    poolLayoutRepositoryMock
        .Setup(asyncFindAndPaginateSignature)
        .Returns(
            Task.FromResult(
                { CursorPage.NextToken = None
                  Items =
                      [| { PoolLayoutEntity.PoolLayoutId = Ulid.newUlid () |> Ulid.toString
                           Name = "Copa América 2021"
                           StartOpeningDateTime = DateTime.Now
                           EndOpeningDateTime = DateTime.Now
                           Pk = ""
                           Sk = ""
                           Filter = "" } |] }
            )
            |> Async.AwaitTask
        )
    |> ignore

    let mapFunc =
        fun _ ->
            { PoolLayout.PoolLayoutPK = { PoolLayoutPK.PoolLayoutId = Ulid.newUlid () }
              Name = NonEmptyString100.From "Copa América 2021"
              OpeningStartDateTime = DateTime.now ()
              OpeningEndDateTime = DateTime.now () }

    let findActivePoolsLayoutsUseCase =
        FindActivePoolsLayoutsUseCase(poolLayoutRepositoryMock.Object, mapFunc) :> IFindActivePoolsLayoutsUseCase

    let _ =
        findActivePoolsLayoutsUseCase.AsyncExecute(None, None)
        |> Async.RunSynchronously

    poolLayoutRepositoryMock.Verify(asyncFindAndPaginateSignature, Times.Once)
