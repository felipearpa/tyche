namespace Pipel.Data.Test.EntityFrameworkCore

open System.Linq
open Pipel.Core
open Pipel.Data.EntityFrameworkCore.Repository
open Pipel.Data.Query
open Data
open DemoData
open Xunit

module FindAndPaginateTests =

    [<Fact>]
    let ``given several items that exist when they are queried and filtered by any criteria and paginated then a paged result is returned with the items that meet the criteria``
        ()
        =
        let products = createProducts ()
        let context = createContextWithData products

        let supplier = "Bavaria"

        let productsFilteredBefore =
            products.Where(fun x -> x.Supplier = supplier)

        let page =
            context.Products
            |> asyncFindWithPagination (QueryString.filterBy ("Supplier = @0", [| supplier |])) 0 products.Length
            |> Async.RunSynchronously

        Assert.Equal(productsFilteredBefore.Count(), page.Items.Count())

        for productFilteredAfter in page.Items do
            Assert.True(productsFilteredBefore.Contains(productFilteredAfter))

    [<Fact>]
    let ``given several items that exist when they are queried and ordered by any criteria with a StringQueryObject and paginated then a paged result is returned with the ordered items``
        ()
        =
        let products = createProducts ()
        let context = createContextWithData products

        let productsOrderedBefore =
            products
                .OrderByDescending(fun x -> x.Price)
                .ThenBy(fun x -> x.Supplier)

        let page =
            context.Products
            |> asyncFindWithPagination (QueryString.orderBy "Price desc, Supplier asc") 0 products.Length
            |> Async.RunSynchronously

        let mutable i = 0

        for productOrderedAfter in page.Items do
            Assert.Equal(productsOrderedBefore.ElementAt(i), productOrderedAfter)
            i <- i + 1

    [<Fact>]
    let ``given several items that exist when they are queried and grouped by any criteria and paginated then a paged result is returned with the grouped items``
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

        let page =
            context.Products
            |> asyncFindWithPagination
                (QueryString.groupBy "Supplier"
                 >> QueryString.newByWithOutTyped<{| Count: int
                                                     Supplier: string |}>
                     "new(Count() as Count, Key as Supplier)")
                0
                products.Length
            |> Async.RunSynchronously

        Assert.Equal(productsGroupedBefore.Count(), page.Items.Count())

        for item in page.Items do
            Assert.NotNull(
                item
                    .GetType()
                    .GetProperty("Count")
                    .GetValue(item, null)
            )

            Assert.True(productsGroupedBefore.Contains(item))
