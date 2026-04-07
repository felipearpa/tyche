namespace Felipearpa.Tyche.Account.Application

open Felipearpa.Tyche.Account.Domain
open Felipearpa.Type

type GetAccountById(accountRepository: IAccountRepository) =
    interface IGetAccountById with
        member this.ExecuteAsync(id: Ulid) = accountRepository.GetByIdAsync id
