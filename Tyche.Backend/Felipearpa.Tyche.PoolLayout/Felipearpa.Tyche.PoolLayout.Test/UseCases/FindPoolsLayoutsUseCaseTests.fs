module FindPoolsLayoutsUseCaseTests

open System
open System.Threading.Tasks
open Moq
open Felipearpa.Core
open Felipearpa.Data.DynamoDb
open Felipearpa.Tyche.PoolLayout.Data
open Felipearpa.Tyche.PoolLayout.Domain
open Felipearpa.Tyche.PoolLayout.Domain.UseCases
open Felipearpa.Type
open Xunit

[<Fact>]
let ``given an empty next token and an empty filter when FindActivePoolsLayoutsUseCase is executed then all of active PoolLayout are returned``
    ()
    =
    let poolLayoutRepositoryMock =
        Mock<IPoolLayoutRepository>()

    let asyncFindAndPaginateSignature =
        FuncAs.LinqExpression (fun (repository: IPoolLayoutRepository) ->
            repository.AsyncFind(It.IsAny<string option>(), It.IsAny<string option>(), It.IsAny<ScanFilter option>()))

    poolLayoutRepositoryMock
        .Setup(asyncFindAndPaginateSignature)
        .Returns(
            Task.FromResult(
                { CursorPage.NextToken = None
                  Items =
                    [| { PoolLayoutEntity.PoolLayoutId = Ulid.random () |> Ulid.value
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
            { PoolLayout.PoolLayoutPK = { PoolLayoutPK.PoolLayoutId = Ulid.random () }
              Name = NonEmptyString100.newOf "Copa América 2021"
              OpeningStartDateTime = DateTime.Now
              OpeningEndDateTime = DateTime.Now }

    let findActivePoolsLayoutsUseCase =
        FindActivePoolsLayoutsUseCase(poolLayoutRepositoryMock.Object, mapFunc) :> IFindActivePoolsLayoutsUseCase

    let _ =
        findActivePoolsLayoutsUseCase.AsyncExecute(None, None)
        |> Async.RunSynchronously

    poolLayoutRepositoryMock.Verify(asyncFindAndPaginateSignature, Times.Once)
