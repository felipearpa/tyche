namespace Felipearpa.Tyche.Function.Request

open Felipearpa.Tyche.Account.Application
open Felipearpa.Type

module LinkAccountRequestTransformer =
    type LinkAccountRequest with

        member this.ToLinkAccountInput() =
            { LinkAccountInput.Email = this.Email |> Email.newOf
              ExternalAccountId = this.ExternalAccountId }
