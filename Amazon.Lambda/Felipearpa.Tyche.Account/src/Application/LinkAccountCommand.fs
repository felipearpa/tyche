namespace Felipearpa.Tyche.Account.Application

open Felipearpa.Tyche.Account.Application.LinkAccountCommandTransformer
open Felipearpa.Tyche.Account.Domain

type LinkAccountCommand(userRepository: IAccountRepository) =

    member this.ExecuteAsync(loginCommand: LinkAccountCommandInput) : Result<Account, unit> Async =
        async { return! userRepository.LinkAsync(loginCommand.ToAccountLink()) }
