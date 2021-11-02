module DemoData

open System
open Data
open Microsoft.EntityFrameworkCore

let createContext () =
    new Context(
        DbContextOptionsBuilder<Context>()
            .UseInMemoryDatabase(
            Guid.NewGuid().ToString()
        )
            .Options
    )

let createProducts () =
    [| { Product.Id = Guid.NewGuid()
         Name = "Bottle of water 300 ml"
         Price = 1.1
         Supplier = "Lotto" }
       { Product.Id = Guid.NewGuid()
         Name = "Bottle of water 600 ml"
         Price = 0.9
         Supplier = "Lotto" }
       { Product.Id = Guid.NewGuid()
         Name = "Heineken beer 350 ml"
         Price = 1.0
         Supplier = "Amm" }
       { Product.Id = Guid.NewGuid()
         Name = "Poker beer"
         Price = 0.8
         Supplier = "Bavaria" }
       { Product.Id = Guid.NewGuid()
         Name = "Coca cola 1000 ml"
         Price = 1.1
         Supplier = "Bavaria" } |]

let createContextWithData (products: Product []) =
    let context = createContext ()
    context.Products.AddRange(products)
    context.SaveChanges() |> ignore
    context
