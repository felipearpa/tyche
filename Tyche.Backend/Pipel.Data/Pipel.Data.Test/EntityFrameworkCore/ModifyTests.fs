namespace Pipel.Data.Test.EntityFrameworkCore

open System.Linq
open Pipel.Core
open Pipel.Data.EntityFrameworkCore.Repository
open Data
open DemoData
open Xunit

module RepositoryTests =

    [<Fact>]
    let ``given an item that exist when it's mark to persist and the changes are saved then the item is persisted`` () =
        let products = createProducts ()
        let context = createContextWithData products

        let productToAdd =
            { Product.Id = products.ElementAt(0).Id
              Name = "Red Bull 500 ml"
              Price = 3.0
              Supplier = "Bavaria" }

        Assert.Throws<AlreadyExistException>
            (fun () ->
                context
                |> asyncAddMany [| productToAdd |]
                |> Async.RunSynchronously
                |> ignore)
        |> ignore

    [<Fact>]
    let ``given an item that exists when it's marked to update and the changes are saved then the item is updated`` () =
        let products = createProducts ()
        let context = createContextWithData products

        let productToUpdate =
            { products.ElementAt(0) with
                  Price = 2.0 }

        let updatedProducts =
            context
            |> asyncUpdateMany [| productToUpdate |]
            |> Async.RunSynchronously

        let updatedProduct = updatedProducts.ElementAt(0)

        Assert.Equal(productToUpdate, updatedProduct)

    [<Fact>]
    let ``given an item that exists when it's marked to remove and the changes are saved then the item is removed`` () =
        let products = createProducts ()
        let context = createContextWithData products

        let countBeforeRemove = context.Products.Count()

        let productToRemove = products.ElementAt(0)

        context.Products
        |> asyncRemoveMany [| productToRemove |]
        |> Async.RunSynchronously

        context.SaveChanges() |> ignore

        let countAfterRemove = context.Products.Count()

        Assert.Equal(countBeforeRemove - 1, countAfterRemove)
