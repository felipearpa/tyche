namespace Felipearpa.Tyche.Function.Response

open Felipearpa.Type
open Felipearpa.Tyche.Account.Domain

module AccountTransformer =
    type Account with

        member this.ToAccountResponse() =
            { AccountResponse.AccountId = this.AccountId |> Ulid.value
              Email = this.Email |> Email.value
              ExternalAccountId = this.ExternalAccountId |> NonEmptyString.value }
