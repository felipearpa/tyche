namespace Pipel.Tyche.PoolLayout.Data

open MongoDB.Driver
open Pipel.Core
open Pipel.Data
open Pipel.Data.MongoDb.Repository

[<Interface>]
type IPoolLayoutRepository =
    inherit IReaderRepository<PoolLayoutEntity, PoolLayoutEntityPK>

type PoolLayoutRepository(context: IMongoContext) =

    [<Literal>]
    let collectionName = "PoolLayout"

    interface IPoolLayoutRepository with

        member this.AsyncFind(queryFunc) =
            async {
                return!
                    context.GetCollection<PoolLayoutEntity>(collectionName)
                    |> asyncFind queryFunc
            }

        member this.AsyncFindAndPaginate(queryFunc, skip, take) =
            async {
                return!
                    context.GetCollection<PoolLayoutEntity>(collectionName)
                    |> asyncFindWithPagination queryFunc skip take
            }

        member this.AsyncFindByKey(key) =
            async {
                let! poolsLayouts =
                    context
                        .GetCollection<PoolLayoutEntity>(collectionName)
                        .FindAsync(fun item -> item.PoolLayoutId.Equals(key.PoolLayoutId))
                    |> Async.AwaitTask

                let! poolLayout =
                    poolsLayouts.FirstOrDefaultAsync()
                    |> Async.AwaitTask

                if isNull <| (poolLayout |> box) then
                    NotFoundException($"PoolLayout with key {key} wasn't found")
                    |> raise

                return poolLayout
            }
