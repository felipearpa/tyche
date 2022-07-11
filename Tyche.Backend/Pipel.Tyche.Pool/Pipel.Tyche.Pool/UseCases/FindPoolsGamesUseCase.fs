namespace Pipel.Tyche.Pool.Domain.UseCases

open Pipel.Core
open Pipel.Data
open Pipel.Data.UseCases
open Pipel.Tyche.Pool.Domain
open Pipel.Tyche.Pool.Data
open Pipel.Type

type IFindPoolsGamesUseCase =

    abstract AsyncExecute: PoolPK * string option * string option -> Async<PoolGame CursorPage>

type FindPoolsGamesUseCase
    (
        poolGameRepository: IPoolGameRepository,
        mapFromDataToDomainFunc: MapFunc<PoolGameEntity, PoolGame>
    ) =

    interface IFindPoolsGamesUseCase with

        member this.AsyncExecute(poolPK, filterText, next) =
            async {
                return!
                    asyncFindWithCursorPagination next mapFromDataToDomainFunc (fun next ->
                        poolGameRepository.AsyncFind(
                            { PoolEntityPK.PoolId = poolPK.PoolId |> Ulid.toString },
                            filterText,
                            next
                        ))
            }
