namespace Felipearpa.Tyche.Account.Application.SignIn

open Felipearpa.Tyche.Account.Domain

module LinkAccountCommandTransformer =
    type LinkAccountCommandInput with

        member this.ToAccountLink() =
            { AccountLink.Email = this.Email
              ExternalAccountId = this.ExternalAccountId }
