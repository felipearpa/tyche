namespace Felipearpa.Tyche.Pool.Application

open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

type RemovePoolGamblerFailure =
    | PoolNotFound
    | NotOwner
    | CannotRemoveOwner

type RemovePoolGambler(poolRepository: IPoolRepository) =

    member this.ExecuteAsync
        (poolId: Ulid, callerGamblerId: Ulid, gamblerId: Ulid)
        : Result<unit, RemovePoolGamblerFailure> Async =
        async {
            let! poolResult = poolRepository.GetPoolByIdAsync poolId

            match poolResult with
            | Error _ -> return failwith "Error querying the pool data"
            | Ok None -> return Error RemovePoolGamblerFailure.PoolNotFound
            | Ok(Some pool) when pool.CreatorGamblerId <> callerGamblerId -> return Error RemovePoolGamblerFailure.NotOwner
            | Ok(Some pool) when pool.CreatorGamblerId = gamblerId -> return Error RemovePoolGamblerFailure.CannotRemoveOwner
            | Ok(Some _) ->
                do! poolRepository.RemoveGamblerAsync(poolId, gamblerId)
                return Ok()
        }
