namespace Felipearpa.Tyche.Pool.Application

open Felipearpa.Tyche.Account.Application
open Felipearpa.Tyche.Account.Domain
open Felipearpa.Tyche.Pool.Domain

type UpdateUsernameFailure = AccountUpdateFailed

type UpdateUsername(accountRepository: IAccountRepository, poolRepository: IPoolRepository) =

    member this.ExecuteAsync(input: UpdateUsernameInput) : Result<unit, UpdateUsernameFailure> Async =
        async {
            let! accountResult = accountRepository.UpdateUsernameAsync(input.AccountId, input.Username)

            match accountResult with
            | Error _ -> return Error AccountUpdateFailed
            | Ok _ ->
                do! poolRepository.PropagateGamblerUsernameAsync(input.AccountId, input.Username)
                return Ok()
        }
