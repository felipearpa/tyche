namespace Felipearpa.Tyche.Pool.Application

open Felipearpa.Tyche.Pool.Domain

type DeletePoolFailure =
    | PoolNotFound
    | NotOwner

type DeletePool(poolRepository: IPoolRepository) =

    member this.ExecuteAsync(input: DeletePoolInput) : Result<unit, DeletePoolFailure> Async =
        async {
            let! poolResult = poolRepository.GetPoolByIdAsync input.PoolId

            match poolResult with
            | Error _ -> return failwith "Error querying the pool data"
            | Ok None -> return Error DeletePoolFailure.PoolNotFound
            | Ok(Some pool) when pool.CreatorGamblerId <> input.GamblerId -> return Error DeletePoolFailure.NotOwner
            | Ok(Some _) ->
                do! poolRepository.DeletePoolAsync input.PoolId
                return Ok()
        }
