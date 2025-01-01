namespace Felipearpa.Tyche.Account.Api.Request.SignIn

open Felipearpa.Type
open Felipearpa.Tyche.Account.Application.SignIn

module SignInRequestTransformer =
    type LinkAccountRequest with

        member this.ToLinkAccountCommand() =
            { LinkAccountCommand.Email = this.Email |> Email.newOf
              ExternalAccountId = this.ExternalAccountId }
