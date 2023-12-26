namespace Felipearpa.Account.Application.SignIn

open Felipearpa.Account.Application
open Felipearpa.Account.Application.AccountTransformer
open Felipearpa.Account.Application.SignIn.LinkAccountCommandTransformer
open Felipearpa.Account.Domain

type LoginCommandHandler(userRepository: IAccountRepository) =

    member this.ExecuteAsync(loginCommand: LinkAccountCommand) : Result<AccountViewModel, unit> Async =
        async {
            let! result = userRepository.RegisterAsync(loginCommand.ToAccountLink())

            return result |> Result.map (fun account -> account.ToAccountViewModel())
        }
