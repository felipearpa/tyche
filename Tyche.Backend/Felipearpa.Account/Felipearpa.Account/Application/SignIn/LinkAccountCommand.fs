namespace Felipearpa.Account.Application.SignIn

open Felipearpa.Type

type LinkAccountCommand = { Email: Email; ExternalAccountId: string }
