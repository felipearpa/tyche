namespace Felipearpa.Tyche.Account.Domain

open Felipearpa.Type

module AccountLinkTransformer =
    [<Literal>]
    let prefixAccount = "ACCOUNT"

    let toAccountEntity (accountLink: AccountLink) =
        let accountId = Ulid.random().ToString()

        { AccountEntity.Pk = $"{prefixAccount}#{accountId}"
          AccountId = accountId
          Email = accountLink.Email |> Email.value
          ExternalAccountId = accountLink.ExternalAccountId }

    type AccountLink with

        member this.ToAccountEntity() = toAccountEntity this
