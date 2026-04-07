namespace Felipearpa.Tyche.Account.Application

open Felipearpa.Type

type LinkAccountInput =
    { Email: Email
      ExternalAccountId: string }
