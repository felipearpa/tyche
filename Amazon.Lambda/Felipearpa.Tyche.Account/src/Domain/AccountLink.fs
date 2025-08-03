namespace Felipearpa.Tyche.Account.Domain

open Felipearpa.Type

type AccountLink =
    { Email: Email
      ExternalAccountId: string }
