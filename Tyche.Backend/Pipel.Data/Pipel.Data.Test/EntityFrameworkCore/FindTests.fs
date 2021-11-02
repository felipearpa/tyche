namespace Pipel.Data.Test.EntityFrameworkCore

open System.Linq
open Pipel.Data.EntityFrameworkCore.Repository
open Pipel.Data.Query
open Data
open DemoData
open Xunit

module FindTests =

    [<Fact>]
    let ``given several items that exist when they are queried and filtered by any criteria then the items that meet the criteria are returned``
        ()
        =
        let products = createProducts ()
        let context = createContextWithData products

        let supplier = "Bavaria"

        let productsFilteredBefore =
            products.Where(fun x -> x.Supplier = supplier)

        let items =
            context.Products
            |> asyncFind (QueryString.filterBy ("Supplier = @0", [| supplier |]))
            |> Async.RunSynchronously

        Assert.Equal(productsFilteredBefore.Count(), items.Count())

    [<Fact>]
    let ``given several items that exist when they are queried and ordered by any criteria with a StringQueryObject then the ordered items are returned``
        ()
        =
        let products = createProducts ()
        let context = createContextWithData products

        let productsOrderedBefore =
            products
                .OrderByDescending(fun x -> x.Price)
                .ThenBy(fun x -> x.Supplier)

        let items =
            context.Products
            |> asyncFind (QueryString.orderBy "Price desc, Supplier asc")
            |> Async.RunSynchronously
            |> Seq.cast<Product>

        let mutable i = 0

        for productOrderedAfter in items do
            Assert.Equal(productsOrderedBefore.ElementAt(i), productOrderedAfter)
            i <- i + 1

    [<Fact>]
    let ``given several items that exist when they are queried and grouped by any criteria then the grouped items are returned``
        ()
        =
        let products = createProducts ()
        let context = createContextWithData products

        let productsGroupedBefore =
            products
                .GroupBy(fun x -> x.Supplier)
                .Select(fun x ->
                    {| Supplier = x.Key
                       Count = x.Count() |})

        let items =
            context.Products
            |> asyncFind (
                QueryString.groupBy "Supplier"
                >> QueryString.newByWithOutTyped<{| Count: int
                                                    Supplier: string |}>
                    "new(Count() as Count, Key as Supplier)"
            )
            |> Async.RunSynchronously

        Assert.Equal(productsGroupedBefore.Count(), items.Count())

        for item in items do
            Assert.True(productsGroupedBefore.Contains(item))
