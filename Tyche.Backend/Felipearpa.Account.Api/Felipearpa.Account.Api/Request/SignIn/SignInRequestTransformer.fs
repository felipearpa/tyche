namespace Felipearpa.Account.Api.Request.SignIn

open Felipearpa.Type
open Felipearpa.Account.Application.SignIn

module SignInRequestTransformer =
    type LinkAccountRequest with

        member this.ToLinkAccountCommand() =
            { LinkAccountCommand.Email = this.Email |> Email.newOf
              ExternalAccountId = this.ExternalAccountId }
