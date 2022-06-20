module Data

open System
open System.ComponentModel.DataAnnotations

[<CLIMutable>]
type Product =
    { [<Key>]
      Id: Guid
      Name: string
      Price: float
      Supplier: string }
