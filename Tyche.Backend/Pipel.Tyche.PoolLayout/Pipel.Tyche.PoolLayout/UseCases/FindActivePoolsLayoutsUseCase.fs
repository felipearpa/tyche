namespace Pipel.Tyche.PoolLayout.Domain.UseCases

open Pipel.Core
open Pipel.Data
open Pipel.Data.Query
open Pipel.Data.UseCases
open Pipel.Tyche.PoolLayout.Domain
open Pipel.Tyche.PoolLayout.Domain.UseCases.Queries
open Pipel.Tyche.PoolLayout.Data
open Pipel.Type

[<Interface>]
type IFindActivePoolsLayoutsUseCase =

    abstract AsyncExecute : NonEmptyString option * int * int -> Async<PoolLayout Page>

type FindActivePoolsLayoutsUseCase
    (
        poolLayoutRepository: IPoolLayoutRepository,
        mapFromDataToDomainFunc: MapFunc<PoolLayoutEntity, PoolLayout>
    ) =

    interface IFindActivePoolsLayoutsUseCase with

        member this.AsyncExecute(filter: NonEmptyString option, skip: int, take: int) =
            async {
                let queryFunc =
                    QueryFunc.normalize (ActivePoolLayoutQuery.execute filter)

                return!
                    poolLayoutRepository
                    |> asyncFindAndPaginate queryFunc skip take mapFromDataToDomainFunc
            }
