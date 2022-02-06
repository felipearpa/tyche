namespace Pipel.Tyche.PoolLayout.Domain.UseCases

open System
open Amazon.DynamoDBv2.Model
open Pipel.Core
open Pipel.Core.DateTimeExtensions
open Pipel.Data
open Pipel.Data.UseCases
open Pipel.Tyche.PoolLayout.Domain
open Pipel.Tyche.PoolLayout.Data

[<Interface>]
type IFindActivePoolsLayoutsUseCase =

    abstract AsyncExecute : string option * string option -> Async<PoolLayout CursorPage>

type FindActivePoolsLayoutsUseCase
    (
        poolLayoutRepository: IPoolLayoutRepository,
        mapFromDataToDomainFunc: MapFunc<PoolLayoutEntity, PoolLayout>
    ) =

    interface IFindActivePoolsLayoutsUseCase with

        member this.AsyncExecute(filterText, next) =
            async {
                let nowIsoString =
                    DateTime.Now.ToUniversalTime().ToISOString()

                return!
                    asyncFindWithCursorPagination
                        next
                        mapFromDataToDomainFunc
                        (fun next ->
                            poolLayoutRepository.AsyncFindWithCursorPagination(
                                filterText,
                                next,
                                (("and #startOpeningDateTime <= :startOpeningDateTime and #endOpeningDateTime > :endOpeningDateTime",
                                  dict [ ":startOpeningDateTime", AttributeValue(nowIsoString)
                                         ":endOpeningDateTime", AttributeValue(nowIsoString) ],
                                  dict [ "#startOpeningDateTime", "startOpeningDateTime"
                                         "#endOpeningDateTime", "endOpeningDateTime" ]
                                  |> Some)
                                 |> Some)
                            ))
            }
