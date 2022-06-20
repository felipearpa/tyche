namespace Pipel.Tyche.Pool.Domain.UseCases

open Pipel.Core
open Pipel.Data
open Pipel.Data.UseCases
open Pipel.Tyche.Pool.Domain
open Pipel.Tyche.Pool.Data
open Pipel.Type

type IFindPoolsGamblersUseCase =

    abstract AsyncExecute : PoolPK * string option * string option -> Async<PoolGambler CursorPage>

type FindPoolsGamblersUseCase(poolGamblerRepository: IPoolGamblerRepository, mapFromDataToDomainFunc: MapFunc<PoolGamblerEntity, PoolGambler>) =

    interface IFindPoolsGamblersUseCase with

        member this.AsyncExecute(poolPK, filterText, next) =
            async {
                return!
                    asyncFindWithCursorPagination
                        next
                        mapFromDataToDomainFunc
                        (fun next ->
                            poolGamblerRepository.AsyncFindWithCursorPagination(
                                { PoolEntityPK.PoolId = poolPK.PoolId |> Ulid.toString },
                                filterText,
                                next
                            ))
            }
