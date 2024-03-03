namespace Felipearpa.Account.Domain

open Felipearpa.Type

module AccountEntityTransformer =
    type AccountEntity with

        member this.ToAccount() =
            { Account.AccountId = Ulid.newOf this.AccountId
              Email = Email.newOf this.Email
              ExternalAccountId = NonEmptyString.newOf this.ExternalAccountId }
