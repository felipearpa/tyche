namespace Felipearpa.Tyche.Pool.Application

open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Account.Application
open Felipearpa.Type

type JoinPoolFailure =
    | AlreadyJoined
    | PoolNotFound
    | GamblerNotFound

type JoinPoolCommand(poolRepository: IPoolRepository, getAccountById: IGetAccountById) =

    member this.ExecuteAsync(joinPoolInput: JoinPoolInput) : Result<unit, JoinPoolFailure> Async =
        async {
            let! accountResult = getAccountById.ExecuteAsync joinPoolInput.GamblerId
            let! poolResult = poolRepository.GetPoolByIdAsync joinPoolInput.PoolId

            match accountResult, poolResult with
            | Ok(Some account), Ok(Some pool) ->
                let! result =
                    poolRepository.JoinPoolAsync
                        { ResolvedJoinPoolInput.PoolLayoutId = pool.PoolLayoutId
                          PoolId = joinPoolInput.PoolId
                          PoolName = pool.PoolName
                          GamblerId = joinPoolInput.GamblerId
                          GamblerUsername = account.Email |> Email.value |> NonEmptyString100.newOf }

                return
                    match result with
                    | Ok _ -> Ok()
                    | Error _ -> JoinPoolFailure.AlreadyJoined |> Error
            | Ok(Some _), Ok None -> return JoinPoolFailure.PoolNotFound |> Error
            | Ok None, Ok(Some _) -> return JoinPoolFailure.GamblerNotFound |> Error
            | _, _ -> return failwith "Unexpected state"
        }
