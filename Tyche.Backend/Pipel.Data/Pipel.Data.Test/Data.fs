module Data

open System
open System.ComponentModel.DataAnnotations
open Microsoft.EntityFrameworkCore

type ProductPK = { Id: Guid }

[<CLIMutable>]
type Product =
    { [<Key>]
      Id: Guid
      Name: string
      Price: float
      Supplier: string }

type Context(options: DbContextOptions<Context>) =
    inherit DbContext(options)

    [<DefaultValue>]
    val mutable private products: DbSet<Product>

    member this.Products
        with get () = this.products
        and set v = this.products <- v
