namespace Felipearpa.Account.Domain

open Felipearpa.Type

type AccountLink =
    { Email: Email
      ExternalAccountId: string }
