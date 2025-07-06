namespace Felipearpa.Tyche.Pool.Application

open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Account.Application
open Felipearpa.Type

type CreatePoolCommand(poolRepository: IPoolRepository, getAccountById: IGetAccountById) =

    member this.ExecuteAsync(createPoolInput: CreatePoolInput) =
        async {
            let! accountResult = getAccountById.ExecuteAsync createPoolInput.OwnerGamblerId

            return!
                match accountResult with
                | Ok maybeAccount ->
                    match maybeAccount with
                    | None -> failwith "Account doesn't exist"
                    | Some account ->
                        poolRepository.createPool
                            { ResolvedCreatePoolInput.PoolLayoutId = createPoolInput.PoolLayoutId
                              PoolId = createPoolInput.PoolId
                              PoolName = createPoolInput.PoolName
                              OwnerGamblerId = createPoolInput.OwnerGamblerId
                              OwnerGamblerUsername = account.Email |> Email.value |> NonEmptyString100.newOf }
                | Error _ -> failwith "Error querying the account data"
        }
