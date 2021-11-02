module QueryUseCasesTests

open System
open System.Threading.Tasks
open Moq
open Pipel.Core
open Pipel.Data
open Pipel.Data.Query
open Pipel.Data.UseCases
open Data
open Xunit

[<Fact>]
let ``given an item that exist when an action to find an item by its primary key is executed then the item is returned``
    ()
    =
    let repositoryMock =
        Mock<IReaderRepository<Product, ProductPK>>()

    let productPK = { ProductPK.Id = Guid.NewGuid() }

    repositoryMock
        .Setup(fun x -> x.AsyncFindByKey(It.IsAny<ProductPK>()))
        .Returns(
            Task.FromResult(
                { Product.Id = productPK.Id
                  Name = "Bottle of water 300 ml"
                  Price = 0.5
                  Supplier = "Lotto" }
            )
            |> Async.AwaitTask
        )
    |> ignore

    let _ =
        repositoryMock.Object
        |> asyncFindByKey productPK id
        |> Async.RunSynchronously

    repositoryMock.Verify((fun x -> x.AsyncFindByKey(It.IsAny<ProductPK>())), Times.Once)

[<Fact>]
let ``given several items that exist when an action to query them is executed then a paged result is returned`` () =
    let repositoryMock =
        Mock<IReaderRepository<Product, ProductPK>>()

    repositoryMock
        .Setup(fun x -> x.AsyncFind(It.IsAny<QueryFunc>()))
        .Returns(
            Task.FromResult(
                [| { Product.Id = Guid.NewGuid()
                     Name = "Bottle of water 300 ml"
                     Price = 0.5
                     Supplier = "Lotto" } |]
                |> Array.toSeq
            )
            |> Async.AwaitTask
        )
    |> ignore

    let _: seq<Product> =
        repositoryMock.Object
        |> asyncFind (QueryString.filterBy ("Supplier = @0", [| "Lotto" |])) id
        |> Async.RunSynchronously

    repositoryMock.Verify((fun x -> x.AsyncFind(It.IsAny<QueryFunc>())), Times.Once)

[<Fact>]
let ``given several items that exist when an action to query and paginate them is executed then a paged result is returned``
    ()
    =
    let repositoryMock =
        Mock<IReaderRepository<Product, ProductPK>>()

    repositoryMock
        .Setup(fun x -> x.AsyncFindAndPaginate(It.IsAny<QueryFunc>(), It.IsAny<int>(), It.IsAny<int>()))
        .Returns(
            Task.FromResult(
                { Page.ItemsCount = 1
                  Skip = 0
                  Take = 1
                  HasNext = false
                  Items =
                      [| { Product.Id = Guid.NewGuid()
                           Name = "Bottle of water 300 ml"
                           Price = 0.5
                           Supplier = "Lotto" } |]
                      |> Array.toSeq }
            )
            |> Async.AwaitTask
        )
    |> ignore

    let _: Page<Product> =
        repositoryMock.Object
        |> asyncFindAndPaginate (QueryString.filterBy ("Supplier = @0", [| "Lotto" |])) 0 1 id
        |> Async.RunSynchronously

    repositoryMock.Verify(
        (fun x -> x.AsyncFindAndPaginate(It.IsAny<QueryFunc>(), It.IsAny<int>(), It.IsAny<int>())),
        Times.Once
    )
