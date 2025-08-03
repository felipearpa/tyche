namespace Felipearpa.Tyche.Account.Domain

open Felipearpa.Type

module AccountTransformer =
    [<Literal>]
    let prefixAccount = "ACCOUNT"

    type Account with

        member this.ToAccountEntity() =
            { AccountEntity.Pk = $"{prefixAccount}#{this.AccountId}"
              AccountId = this.AccountId |> Ulid.value
              Email = this.Email |> Email.value
              ExternalAccountId = this.ExternalAccountId |> NonEmptyString.value }
