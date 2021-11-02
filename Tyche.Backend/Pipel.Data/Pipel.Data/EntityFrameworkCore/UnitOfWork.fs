namespace Pipel.Data.EntityFrameworkCore

open Microsoft.EntityFrameworkCore
open Pipel.Data

type UnitOfWork(context: DbContext) =
    interface IUnitOfWork with

        member this.SaveChanges() = context.SaveChanges() |> ignore
