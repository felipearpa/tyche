namespace Pipel.Data

open Pipel.Core

module UseCases =

    let private convertItemIfDataModelType<'TModel, 'TEntity when 'TModel: not struct and 'TEntity: not struct>
        (mapFromDataToDomainFunc: MapFunc<'TEntity, 'TModel>)
        (obj: obj)
        : obj =
        match obj with
        | :? 'TEntity -> (obj :?> 'TEntity) |> mapFromDataToDomainFunc :> obj
        | _ -> obj

    let asyncFindWithCursorPagination<'TEntity, 'TKey, 'TModel when 'TEntity: not struct and 'TKey: not struct and 'TModel: not struct>
        (next: string option)
        (mapFromDataToDomainFunc: MapFunc<'TEntity, 'TModel>)
        (findWithCursorPaginationAsyncFunc: string option -> Async<'TEntity CursorPage>)
        : Async<'TModel CursorPage> =
        async {
            let! page = findWithCursorPaginationAsyncFunc next

            return
                page
                |> CursorPage.map
                    (fun x ->
                        (x
                         |> convertItemIfDataModelType mapFromDataToDomainFunc)
                        :?> 'TModel)
        }
