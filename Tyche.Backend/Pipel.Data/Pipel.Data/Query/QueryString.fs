namespace Pipel.Data.Query

open System.Linq
open Pipel.Core.IQueryableExtensions

module QueryString =

    let filterBy (whereClause: string * obj []) (query: IQueryable) =
        match whereClause with
        | where, args -> query.Where(where, args)

    let orderBy (orderClause: string) (query: IQueryable) = query.OrderBy orderClause :> IQueryable

    let groupBy (groupClause: string) (query: IQueryable) = query.GroupBy groupClause

    let newBy (selectClause: string) (query: IQueryable) = query.Select selectClause

    let newByWithOutTyped<'TOut> (selectClause: string) (query: IQueryable) =
        query.Select<'TOut> selectClause :> IQueryable
