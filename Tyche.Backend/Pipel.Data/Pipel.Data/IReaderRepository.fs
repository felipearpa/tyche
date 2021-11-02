namespace Pipel.Data

open Pipel.Core
open Pipel.Data.Query

[<Interface>]
type IReaderRepository<'TEntity, 'TKey when 'TEntity: not struct and 'TKey: not struct> =

    abstract AsyncFindByKey : 'TKey -> Async<'TEntity>

    abstract AsyncFind : QueryFunc -> Async<'TEntity seq>

    abstract AsyncFindAndPaginate : QueryFunc * int * int -> Async<'TEntity Page>
