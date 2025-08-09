namespace Felipearpa.Tyche.Pool.Application

open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Account.Application
open Felipearpa.Type

type CreatePoolFailure = | GamblerNotFound

type CreatePoolCommand(poolRepository: IPoolRepository, getAccountById: IGetAccountById) =

    member this.ExecuteAsync(createPoolInput: CreatePoolInput) : Result<CreatePoolOutput, CreatePoolFailure> Async =
        async {
            let! accountResult = getAccountById.ExecuteAsync createPoolInput.OwnerGamblerId

            match accountResult with
            | Error _ -> return failwith "Error querying the account data"

            | Ok None -> return Error CreatePoolFailure.GamblerNotFound

            | Ok(Some account) ->
                let resolvedInput =
                    { ResolvedCreatePoolInput.PoolLayoutId = createPoolInput.PoolLayoutId
                      PoolId = createPoolInput.PoolId
                      PoolName = createPoolInput.PoolName
                      OwnerGamblerId = createPoolInput.OwnerGamblerId
                      OwnerGamblerUsername = account.Email |> Email.value |> NonEmptyString100.newOf }

                let! result = poolRepository.CreatePoolAsync resolvedInput

                return
                    match result with
                    | Ok createPoolOutput -> Ok createPoolOutput
                    | Error _ -> failwith "Error storing the pool"
        }
