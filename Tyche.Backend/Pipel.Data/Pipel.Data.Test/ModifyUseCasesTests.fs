module ModifyUseCasesTests

open System
open System.Threading.Tasks
open Moq
open Pipel.Data
open Pipel.Data.UseCases
open Data
open Xunit

[<Fact>]
let ``given an item that doesn't exist when an action to persist it is executed then the item is persisted`` () =
    let repositoryMock = Mock<IAdderRepository<Product>>()

    let product =
        { Product.Id = Guid.NewGuid()
          Name = "Bottle of water 300 ml"
          Price = 0.5
          Supplier = "Lotto" }

    repositoryMock
        .Setup(fun x -> x.AsyncAddMany(It.IsAny<Product seq>()))
        .Returns(
            Task.FromResult([| product |] |> Array.toSeq)
            |> Async.AwaitTask
        )
    |> ignore

    let _ =
        repositoryMock.Object
        |> asyncAddMany ([| product |] |> Array.toSeq) id id
        |> Async.RunSynchronously

    repositoryMock.Verify((fun x -> x.AsyncAddMany(It.IsAny<Product seq>())), Times.Once)

[<Fact>]
let ``given an item that exists when an action to update it is executed then then the item is updated`` () =
    let repositoryMock = Mock<IUpdaterRepository<Product>>()

    let product =
        { Product.Id = Guid.NewGuid()
          Name = "Bottle of water 300 ml"
          Price = 0.5
          Supplier = "Lotto" }

    repositoryMock
        .Setup(fun x -> x.AsyncUpdateMany(It.IsAny<Product seq>()))
        .Returns(
            Task.FromResult([| product |] |> Array.toSeq)
            |> Async.AwaitTask
        )
    |> ignore

    let productToUpdate =
        { product with
              Price = 1.0 }

    let _ =
        repositoryMock.Object
        |> asyncUpdateMany [| productToUpdate |] id id
        |> Async.RunSynchronously

    repositoryMock.Verify((fun x -> x.AsyncUpdateMany(It.IsAny<Product seq>())), Times.Once)

[<Fact>]
let ``given an item that exists when an action to remove an item by its key is executed then the item is removed`` () =
    let repositoryMock =
        Mock<IRemoverRepository<Product, ProductPK>>()

    let productPK = { ProductPK.Id = Guid.NewGuid() }

    repositoryMock
        .Setup(fun x -> x.AsyncRemoveManyByKeys(It.IsAny<ProductPK seq>()))
        .Returns(Task.FromResult() |> Async.AwaitTask)
    |> ignore

    let _ =
        repositoryMock.Object
        |> asyncRemoveManyByKeys [| productPK |]
        |> Async.RunSynchronously

    repositoryMock.Verify(fun x -> x.AsyncRemoveManyByKeys(It.IsAny<ProductPK seq>()))

[<Fact>]
let ``given an item that exists when an action to remove an item is executed then the item is removed`` () =
    let repositoryMock =
        Mock<IRemoverRepository<Product, ProductPK>>()

    repositoryMock
        .Setup(fun x -> x.AsyncRemoveMany(It.IsAny<Product seq>()))
        .Returns(Task.FromResult() |> Async.AwaitTask)
    |> ignore

    let _ =
        repositoryMock.Object
        |> asyncRemoveMany
            [| { Product.Id = Guid.NewGuid()
                 Name = "Bottle of water 300 ml"
                 Price = 0.5
                 Supplier = "Lotto" } |]
            id
        |> Async.RunSynchronously

    repositoryMock.Verify(fun x -> x.AsyncRemoveMany(It.IsAny<Product seq>()))
