namespace Felipearpa.Tyche.Account.Application.SignIn

open Felipearpa.Type

type LinkAccountCommandInput =
    { Email: Email
      ExternalAccountId: string }
