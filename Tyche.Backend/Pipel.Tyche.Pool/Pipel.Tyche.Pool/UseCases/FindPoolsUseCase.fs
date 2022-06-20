namespace Pipel.Tyche.Pool.Domain.UseCases

open Pipel.Core
open Pipel.Data
open Pipel.Data.UseCases
open Pipel.Tyche.Pool.Domain
open Pipel.Tyche.Pool.Data
open Pipel.Type

type IFindPoolsUseCase =

    abstract AsyncExecute : PoolLayoutPK * string option * string option -> Async<Pool CursorPage>

type FindPoolsUseCase(poolRepository: IPoolRepository, mapFromDataToDomainFunc: MapFunc<PoolEntity, Pool>) =

    interface IFindPoolsUseCase with

        member this.AsyncExecute(poolLayoutPK, filterText, next) =
            async {
                return!
                    asyncFindWithCursorPagination
                        next
                        mapFromDataToDomainFunc
                        (fun next ->
                            poolRepository.AsyncFindWithCursorPagination(
                                { PoolLayoutEntityPK.PoolLayoutId = poolLayoutPK.PoolLayoutId |> Ulid.toString },
                                filterText,
                                next
                            ))
            }
