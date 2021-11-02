namespace Pipel.Data.Test.EntityFrameworkCore

open System.Linq
open Pipel.Data.EntityFrameworkCore
open Pipel.Data
open DemoData
open Xunit

module UnitOfWorkTests =

    [<Fact>]
    let ``given several items when they are mark to persist and the changes are saved then the items are persisted``
        ()
        =
        let products = createProducts ()
        let context = createContext ()
        let unitOfWork = UnitOfWork(context) :> IUnitOfWork

        context.Products.AddRange products
        unitOfWork.SaveChanges()

        Assert.Equal(products.Count(), context.Products.Count())
