namespace Felipearpa.Tyche.Function.Request

open Felipearpa.Tyche.Account.Application.SignIn
open Felipearpa.Type

module LinkAccountRequestTransformer =
    type LinkAccountRequest with

        member this.ToLinkAccountCommandInput() =
            { LinkAccountCommandInput.Email = this.Email |> Email.newOf
              ExternalAccountId = this.ExternalAccountId }
