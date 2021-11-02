namespace Pipel.Data.EntityFrameworkCore

open System
open System.Linq
open Microsoft.EntityFrameworkCore
open Pipel.Core
open Pipel.Data.Query

module Repository =

    let asyncFind<'TEntity, 'TOut when 'TEntity: not struct>
        (queryFunc: QueryFunc)
        (set: DbSet<'TEntity>)
        : Async<'TOut seq> =
        async {
            let query = set.AsNoTracking()

            let outQuery =
                queryFunc |> QueryFunc.runWithOutTyped query

            let! items = outQuery.ToListAsync() |> Async.AwaitTask
            return items :> seq<_>
        }

    let asyncFindWithPagination<'TEntity, 'TOut when 'TEntity: not struct>
        (queryFunc: QueryFunc)
        (skip: int)
        (take: int)
        (set: DbSet<'TEntity>)
        : Async<Page<'TOut>> =
        async {
            let query = set.AsNoTracking()

            let mutable outQuery =
                queryFunc |> QueryFunc.runWithOutTyped query

            let! itemsCount = outQuery.CountAsync() |> Async.AwaitTask
            outQuery <- outQuery.Skip(skip).Take(take)
            let! items = outQuery.ToListAsync() |> Async.AwaitTask

            return
                { Page.Items = items
                  ItemsCount = itemsCount
                  Skip = skip
                  Take =
                      if skip + take <= itemsCount then
                          take
                      else
                          itemsCount
                  HasNext = skip + take < itemsCount }
        }

    let asyncAddMany<'TEntity when 'TEntity: not struct> (items: 'TEntity seq) (context: DbContext) =
        async {
            let mutable addedItems: 'TEntity list = []

            for item in items do
                let entry = context.Entry(item)

                let ids =
                    entry
                        .Metadata
                        .FindPrimaryKey()
                        .Properties.Select(fun x -> entry.Property(x.Name).CurrentValue)
                        .ToArray()

                let! itemToAdd =
                    context.Set<'TEntity>().FindAsync(ids).AsTask()
                    |> Async.AwaitTask

                if box itemToAdd |> isNull |> not then
                    AlreadyExistException(
                        String.Format("The item with ids = ({0}) already exists", String.Join(",", ids))
                    )
                    |> raise

                let! entityEntry =
                    (context.AddAsync(item)).AsTask()
                    |> Async.AwaitTask

                addedItems <- [ (entityEntry.Entity :?> 'TEntity) ] @ addedItems

            return addedItems |> List.toSeq
        }

    let asyncUpdateMany<'TEntity when 'TEntity: not struct> (items: 'TEntity seq) (context: DbContext) =
        async {
            let mutable updatedItems: 'TEntity list = []

            for item in items do
                let entry = context.Entry(item)

                let key =
                    (entry
                        .Metadata
                        .FindPrimaryKey()
                        .Properties.Select(fun x -> entry.Property(x.Name).CurrentValue)
                        .ToArray())

                let! itemToUpdate =
                    context.Set<'TEntity>().FindAsync(key).AsTask()
                    |> Async.AwaitTask

                context
                    .Entry(itemToUpdate)
                    .CurrentValues.SetValues(item)

                let entityEntry = context.Update(itemToUpdate)

                updatedItems <-
                    [ (entityEntry.Entity :?> 'TEntity) ]
                    @ updatedItems

            return updatedItems |> List.toSeq
        }

    let asyncRemoveMany<'TEntity when 'TEntity: not struct> (items: 'TEntity seq) (set: DbSet<'TEntity>) =
        async { set.RemoveRange items }
