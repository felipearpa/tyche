module FindPoolsLayoutsUseCaseTests

open System
open System.Threading.Tasks
open Moq
open Pipel.Core
open Pipel.Data.Query
open Pipel.Tyche.PoolLayout.Data
open Pipel.Tyche.PoolLayout.Domain
open Pipel.Tyche.PoolLayout.Domain.UseCases
open Pipel.Type
open Xunit

[<Fact>]
let ``given an filter text, a skip, and a take when FindActivePoolsLayoutsUseCase is executed then all of active PoolLayout are returned``
    ()
    =
    let poolLayoutRepositoryMock = Mock<IPoolLayoutRepository>()

    let asyncFindAndPaginateSignature =
        FuncAs.LinqExpression
            (fun (repository: IPoolLayoutRepository) ->
                repository.AsyncFindAndPaginate(It.IsAny<QueryFunc>(), It.IsAny<int>(), It.IsAny<int>()))

    poolLayoutRepositoryMock
        .Setup(asyncFindAndPaginateSignature)
        .Returns(
            Task.FromResult(
                { Page.ItemsCount = 1
                  Skip = 0
                  Take = 1
                  HasNext = false
                  Items =
                      [| { PoolLayoutEntity.PoolLayoutId = Guid.NewGuid()
                           Name = "Copa América 2021"
                           OpeningStartDateTime = DateTime.Now
                           OpeningEndDateTime = DateTime.Now } |] }
            )
            |> Async.AwaitTask
        )
    |> ignore

    let mapFunc =
        fun _ ->
            { PoolLayout.PoolLayoutPK = { PoolLayoutPK.PoolLayoutId = Uuid.newUuid () }
              Name = NonEmptyString100.From "Copa América 2021"
              OpeningStartDateTime = DateTime.now ()
              OpeningEndDateTime = DateTime.now () }

    let findActivePoolsLayoutsUseCase =
        FindActivePoolsLayoutsUseCase(poolLayoutRepositoryMock.Object, mapFunc) :> IFindActivePoolsLayoutsUseCase

    let _ =
        findActivePoolsLayoutsUseCase.AsyncExecute(None, 0, 1)
        |> Async.RunSynchronously

    poolLayoutRepositoryMock.Verify(asyncFindAndPaginateSignature, Times.Once)
