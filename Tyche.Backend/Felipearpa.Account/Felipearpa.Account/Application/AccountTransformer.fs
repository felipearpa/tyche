namespace Felipearpa.Account.Application

open Felipearpa.Type
open Felipearpa.Account.Domain

module AccountTransformer =
    type Account with

        member this.ToAccountViewModel() =
            { AccountViewModel.AccountId = this.AccountId |> Ulid.value
              Email = this.Email |> Email.value
              ExternalAccountId = this.ExternalAccountId |> NonEmptyString.value }
