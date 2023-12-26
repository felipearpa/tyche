namespace Felipearpa.Account.Application.SignIn

open Felipearpa.Account.Domain

module LinkAccountCommandTransformer =
    type LinkAccountCommand with

        member this.ToAccountLink() =
            { AccountLink.Email = this.Email
              ExternalAccountId = this.ExternalAccountId }
