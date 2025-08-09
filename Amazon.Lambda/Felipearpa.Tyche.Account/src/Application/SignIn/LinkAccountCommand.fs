namespace Felipearpa.Tyche.Account.Application.SignIn

open Felipearpa.Tyche.Account.Application
open Felipearpa.Tyche.Account.Application.AccountTransformer
open Felipearpa.Tyche.Account.Application.SignIn.LinkAccountCommandTransformer
open Felipearpa.Tyche.Account.Domain

type LinkAccountCommand(userRepository: IAccountRepository) =

    member this.ExecuteAsync(loginCommand: LinkAccountCommandInput) : Result<AccountViewModel, unit> Async =
        async {
            let! result = userRepository.LinkAsync(loginCommand.ToAccountLink())

            return result |> Result.map (fun account -> account.ToAccountViewModel())
        }
