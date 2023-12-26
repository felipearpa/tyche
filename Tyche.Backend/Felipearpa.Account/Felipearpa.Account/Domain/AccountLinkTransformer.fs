namespace Felipearpa.Account.Domain

open Felipearpa.Type

module AccountLinkTransformer =
    [<Literal>]
    let prefixAccount = "ACCOUNT"

    type AccountLink with

        member this.ToAccountEntity() =
            let accountId = Ulid.random().ToString()

            { AccountEntity.Pk = $"{prefixAccount}#{accountId}"
              AccountId = accountId
              Email = this.Email |> Email.value
              ExternalAccountId = this.ExternalAccountId }
