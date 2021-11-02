namespace Pipel.Data

open Pipel.Core
open Pipel.Data.Query

[<Interface>]
type IQueryableRepository<'TEntity> =

    abstract AsyncFind : QueryFunc -> Async<'TEntity seq>

    abstract AsyncFindAndPaginate : QueryFunc * int * int -> Async<'TEntity Page>
