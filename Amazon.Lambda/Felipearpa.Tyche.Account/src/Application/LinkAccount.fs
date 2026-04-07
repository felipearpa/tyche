namespace Felipearpa.Tyche.Account.Application

open Felipearpa.Tyche.Account.Application.LinkAccountTransformer
open Felipearpa.Tyche.Account.Domain

type LinkAccount(accountRepository: IAccountRepository) =

    member this.ExecuteAsync(loginCommand: LinkAccountInput) : Result<Account, unit> Async =
        async { return! accountRepository.LinkAsync(loginCommand.ToAccountLink()) }
