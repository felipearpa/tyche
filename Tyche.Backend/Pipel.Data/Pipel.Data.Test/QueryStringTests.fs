module QueryStringTests

open System.Linq
open Pipel.Data.Query
open Data
open DemoData
open Xunit

[<Fact>]
let ``given several items when they are filtered by any criteria then the items that meet the criteria are returned``
    ()
    =
    let products = createProducts ()
    let supplier = "Bavaria"

    let queryFunc =
        QueryString.filterBy ("Supplier = @0", [| supplier |])

    let filteredItemsUsingQueryObject =
        (queryFunc
         |> QueryFunc.runWithInOutTyped (products.AsQueryable()))
            .ToList()

    let filteredItems =
        products
            .Where(fun x -> x.Supplier.Equals(supplier))
            .ToList()

    Assert.Equal(filteredItems.Count, filteredItemsUsingQueryObject.Count)

[<Fact>]
let ``given several items when they are order by any criteria then the ordered items are returned`` () =
    let products = createProducts ()
    let queryFunc = QueryString.orderBy "Supplier"

    let orderedItemsUsingQueryObject =
        (queryFunc
         |> QueryFunc.runWithInOutTyped (products.AsQueryable()))
            .ToList()

    let orderedItems =
        products.OrderBy(fun x -> x.Supplier).ToList()

    Assert.Equal(orderedItems.Count, orderedItemsUsingQueryObject.Count)

[<Fact>]
let ``given several items when they are group by any criteria then the grouped items are returned`` () =
    let products = createProducts ()

    let queryFunc =
        QueryString.groupBy "Supplier"
        >> QueryString.newBy "key"

    let groupedItemsUsingQueryObject =
        (queryFunc
         |> QueryFunc.runWithOutTyped (products.AsQueryable()))
            .ToList()

    let groupedItems =
        products
            .GroupBy(fun x -> x.Supplier)
            .Select(fun x -> x.Key)
            .ToList()

    Assert.Equal(groupedItems.Count, groupedItemsUsingQueryObject.Count)
