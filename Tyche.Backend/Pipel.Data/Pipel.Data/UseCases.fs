namespace Pipel.Data

open Pipel.Core
open Pipel.Data.Query

module UseCases =

    let private convertItemIfDataModelType<'TModel, 'TEntity when 'TModel: not struct and 'TEntity: not struct>
        (mapFromDataToDomainFunc: MapFunc<'TEntity, 'TModel>)
        (obj: obj)
        : obj =
        match obj with
        | :? 'TEntity -> (obj :?> 'TEntity) |> mapFromDataToDomainFunc :> obj
        | _ -> obj

    let asyncFindByKey<'TEntity, 'TKey, 'TModel when 'TEntity: not struct and 'TKey: not struct and 'TModel: not struct>
        (key: 'TKey)
        (mapFromDataToDomainFunc: MapFunc<'TEntity, 'TModel>)
        (repository: IReaderRepository<'TEntity, 'TKey>)
        : Async<'TModel> =
        async {
            let! item = repository.AsyncFindByKey key
            return item |> mapFromDataToDomainFunc
        }

    let asyncFind<'TEntity, 'TKey, 'TModel when 'TEntity: not struct and 'TKey: not struct and 'TModel: not struct>
        (queryFunc: QueryFunc)
        (mapFromDataToDomainFunc: MapFunc<'TEntity, 'TModel>)
        (repository: IReaderRepository<'TEntity, 'TKey>)
        : Async<'TModel seq> =
        async {
            let! items = repository.AsyncFind queryFunc

            return
                items
                |> Seq.map
                    (fun item ->
                        (item
                         |> convertItemIfDataModelType mapFromDataToDomainFunc)
                        :?> 'TModel)
        }

    let asyncFindAndPaginate<'TEntity, 'TKey, 'TModel when 'TEntity: not struct and 'TKey: not struct and 'TModel: not struct>
        (queryFunc: QueryFunc)
        (skip: int)
        (take: int)
        (mapFromDataToDomainFunc: MapFunc<'TEntity, 'TModel>)
        (repository: IReaderRepository<'TEntity, 'TKey>)
        : Async<'TModel Page> =
        async {
            let! page = repository.AsyncFindAndPaginate(queryFunc, skip, take)

            return
                page
                |> Page.map
                    (fun x ->
                        (x
                         |> convertItemIfDataModelType mapFromDataToDomainFunc)
                        :?> 'TModel)
        }

    let asyncAddMany<'TEntity, 'TModel when 'TEntity: not struct and 'TModel: not struct>
        (items: 'TModel seq)
        (mapFromDomainToDataFunc: MapFunc<'TModel, 'TEntity>)
        (mapFromDataToDomainFunc: MapFunc<'TEntity, 'TModel>)
        (repository: IAdderRepository<'TEntity>)
        : Async<'TModel seq> =
        async {
            let! items =
                repository.AsyncAddMany(
                    items
                    |> Seq.map (fun x -> x |> mapFromDomainToDataFunc)
                )

            return
                items
                |> Seq.map (fun x -> x |> mapFromDataToDomainFunc)
        }

    let asyncUpdateMany<'TEntity, 'TModel when 'TEntity: not struct and 'TModel: not struct>
        (items: 'TModel seq)
        (mapFromDomainToDataFunc: MapFunc<'TModel, 'TEntity>)
        (mapFromDataToDomainFunc: MapFunc<'TEntity, 'TModel>)
        (repository: IUpdaterRepository<'TEntity>)
        : Async<'TModel seq> =
        async {
            let! items =
                repository.AsyncUpdateMany(
                    items
                    |> Seq.map (fun x -> x |> mapFromDomainToDataFunc)
                )

            return
                items
                |> Seq.map (fun x -> x |> mapFromDataToDomainFunc)
        }

    let asyncRemoveManyByKeys<'TEntity, 'TKey, 'TModel when 'TEntity: not struct and 'TKey: not struct and 'TModel: not struct>
        (keys: 'TKey seq)
        (repository: IRemoverRepository<'TEntity, 'TKey>)
        =
        async { do! repository.AsyncRemoveManyByKeys keys }

    let asyncRemoveMany<'TEntity, 'TKey, 'TModel when 'TEntity: not struct and 'TKey: not struct and 'TModel: not struct>
        (items: 'TModel seq)
        (mapFromDomainToDataFunc: MapFunc<'TModel, 'TEntity>)
        (repository: IRemoverRepository<'TEntity, 'TKey>)
        =
        async {
            do!
                repository.AsyncRemoveMany(
                    items
                    |> Seq.map (fun x -> x |> mapFromDomainToDataFunc)
                )
        }
