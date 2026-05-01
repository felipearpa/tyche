namespace Felipearpa.Tyche.Pool.Application

open Felipearpa.Tyche.Pool.Domain
open Felipearpa.Tyche.Account.Application
open Felipearpa.Type

type CreatePoolFailure =
    | GamblerNotFound
    | PoolLayoutNotFound

type CreatePool
    (
        poolRepository: IPoolRepository,
        poolLayoutVersionResolver: IPoolLayoutVersionResolver,
        getAccountById: IGetAccountById
    ) =

    member this.ExecuteAsync(createPoolInput: CreatePoolInput) : Result<CreatePoolOutput, CreatePoolFailure> Async =
        async {
            let! accountResult = getAccountById.ExecuteAsync createPoolInput.OwnerGamblerId
            let! maybeVersion = poolLayoutVersionResolver.ResolveAsync createPoolInput.PoolLayoutId

            match accountResult, maybeVersion with
            | Error _, _ -> return failwith "Error querying the account data"

            | Ok None, _ -> return Error CreatePoolFailure.GamblerNotFound

            | Ok(Some _), None -> return Error CreatePoolFailure.PoolLayoutNotFound

            | Ok(Some account), Some poolLayoutVersion ->
                let resolvedInput =
                    { ResolvedCreatePoolInput.PoolLayoutId = createPoolInput.PoolLayoutId
                      PoolLayoutVersion = poolLayoutVersion
                      PoolId = createPoolInput.PoolId
                      PoolName = createPoolInput.PoolName
                      OwnerGamblerId = createPoolInput.OwnerGamblerId
                      OwnerGamblerUsername = account.Email |> Email.value |> NonEmptyString100.newOf }

                let! result = poolRepository.CreatePoolAsync resolvedInput

                return
                    match result with
                    | Ok createPoolOutput -> Ok createPoolOutput
                    | Error error -> failwith "Error storing the pool"
        }
