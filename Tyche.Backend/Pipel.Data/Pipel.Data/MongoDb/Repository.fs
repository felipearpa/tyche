namespace Pipel.Data.MongoDb

open System.Linq
open MongoDB.Driver
open MongoDB.Driver.Linq
open Pipel.Core
open Pipel.Data.Query

module Repository =

    let asyncFind<'TEntity, 'TOut when 'TEntity: not struct>
        (queryFunc: QueryFunc)
        (collection: IMongoCollection<'TEntity>)
        : Async<'TOut seq> =
        async {
            let query =
                collection.AsQueryable() :> IQueryable<_>

            let outQuery =
                queryFunc |> QueryFunc.runWithOutTyped query

            let! items =
                (outQuery :?> IMongoQueryable<_>).ToListAsync()
                |> Async.AwaitTask

            return items :> seq<_>
        }

    let asyncFindWithPagination<'TEntity, 'TOut when 'TEntity: not struct>
        (queryFunc: QueryFunc)
        (skip: int)
        (take: int)
        (collection: IMongoCollection<'TEntity>)
        : Async<'TOut Page> =
        async {
            let query =
                collection.AsQueryable() :> IQueryable<_>

            let mutable outQuery =
                queryFunc |> QueryFunc.runWithOutTyped query :?> IMongoQueryable<_>

            let! itemsCount = outQuery.CountAsync() |> Async.AwaitTask

            outQuery <- outQuery.Skip(skip).Take(take)

            let! pageCount = outQuery.CountAsync() |> Async.AwaitTask

            let! items = outQuery.ToListAsync() |> Async.AwaitTask

            do! Async.Sleep(2 * 1000)

            return
                { Page.Items = items
                  ItemsCount = pageCount
                  Skip = skip
                  Take =
                      if skip + take <= itemsCount then
                          take
                      else
                          itemsCount
                  HasNext = skip + take < itemsCount }
        }
