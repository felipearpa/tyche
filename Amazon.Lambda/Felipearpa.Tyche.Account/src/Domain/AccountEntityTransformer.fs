namespace Felipearpa.Tyche.Account.Domain

open Felipearpa.Type

module AccountEntityTransformer =
    let toAccount (accountEntity: AccountEntity) =
        { Account.AccountId = Ulid.newOf accountEntity.AccountId
          Email = Email.newOf accountEntity.Email
          ExternalAccountId = NonEmptyString.newOf accountEntity.ExternalAccountId }

    type AccountEntity with

        member this.ToAccount() = toAccount this
