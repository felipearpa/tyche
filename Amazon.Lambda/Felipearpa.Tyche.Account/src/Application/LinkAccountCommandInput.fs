namespace Felipearpa.Tyche.Account.Application

open Felipearpa.Type

type LinkAccountCommandInput =
    { Email: Email
      ExternalAccountId: string }
