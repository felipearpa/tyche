namespace Pipel.Data.Query

open System.Linq

type QueryFunc = IQueryable -> IQueryable

module QueryFunc =

    let run (query: IQueryable) (queryFunc: QueryFunc) = queryFunc query

    let runWithInTyped (query: IQueryable<'T>) (queryFunc: QueryFunc) = queryFunc query

    let runWithOutTyped (query: IQueryable) (queryFunc: QueryFunc) = queryFunc query :?> IQueryable<'T>

    let runWithInOutTyped (query: IQueryable<'T>) (queryFunc: QueryFunc) = queryFunc query :?> IQueryable<'T>

    let normalize (func: IQueryable<'T> -> IQueryable<'T>) (param: IQueryable) =
        param :?> IQueryable<'T> |> func :> IQueryable
