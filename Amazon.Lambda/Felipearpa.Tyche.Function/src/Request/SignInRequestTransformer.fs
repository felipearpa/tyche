namespace Felipearpa.Tyche.Function.Request

open Felipearpa.Tyche.Account.Application.SignIn
open Felipearpa.Type

module SignInRequestTransformer =
    type LinkAccountRequest with

        member this.ToLinkAccountCommand() =
            { LinkAccountCommand.Email = this.Email |> Email.newOf
              ExternalAccountId = this.ExternalAccountId }
