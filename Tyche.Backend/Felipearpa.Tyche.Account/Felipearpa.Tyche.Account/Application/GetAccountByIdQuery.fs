namespace Felipearpa.Tyche.Account.Application

open Felipearpa.Tyche.Account.Domain
open Felipearpa.Type

type GetAccountByIdQuery(accountRepository: IAccountRepository) =
    interface IGetAccountById with
        member this.ExecuteAsync(id: Ulid) = accountRepository.GetById id
