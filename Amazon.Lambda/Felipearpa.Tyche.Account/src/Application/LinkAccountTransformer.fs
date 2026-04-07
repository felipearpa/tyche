namespace Felipearpa.Tyche.Account.Application

open Felipearpa.Tyche.Account.Domain

module LinkAccountTransformer =
    type LinkAccountInput with

        member this.ToAccountLink() =
            { AccountLink.Email = this.Email
              ExternalAccountId = this.ExternalAccountId }
