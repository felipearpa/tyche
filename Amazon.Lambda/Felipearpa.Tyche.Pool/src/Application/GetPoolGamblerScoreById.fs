namespace Felipearpa.Tyche.Pool.Application

open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Type

type GetPoolGamblerScoreById(poolGamblerScoreRepository: IPoolGamblerScoreRepository, poolStandings: PoolStandings) =
    member this.ExecuteAsync(poolId: Ulid, gamblerId: Ulid) : Async<Result<PoolStanding option, unit>> =
        async {
            match! poolGamblerScoreRepository.GetPoolGamblerScoreByIdAsync(poolId, gamblerId) with
            | Ok(Some score) ->
                let! standing = poolStandings.WithTrendGroupAsync score
                return Ok(Some standing)
            | Ok None -> return Ok None
            | Error error -> return Error error
        }
